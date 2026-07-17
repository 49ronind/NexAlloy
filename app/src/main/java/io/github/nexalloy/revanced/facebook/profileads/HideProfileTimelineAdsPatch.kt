package io.github.nexalloy.revanced.facebook.profileads

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * v11 — drop the ad at the STORY-UNIT builder only (no generic render hooks).
 *
 * v10 hooked X.NpX.render / X.2zd.render / X.NpY.A18 and returned null. Those
 * are GENERIC Litho KComponents reused for comments, headers, attachments,
 * etc., so nulling them shredded normal UI (broken comments/thumbnails in the
 * screenshots) while the ad still showed. Those hooks are removed entirely.
 *
 * Correct, surgical chokepoint:
 *
 *   X.2ug.A00(Context, …, X.2nv env, …, GraphQLStory story, …, X.3SA):X.3RU
 *
 * This is the builder for a whole timeline STORY-UNIT component. It:
 *   - takes the story's X.2nv environment AND the GraphQLStory directly,
 *   - returns the unit's root component (X.3RU),
 *   - is called ONLY for story units (X.2ug.A1G does `return A00(...)` with no
 *     null-check), never for comments/headers/attachments.
 *
 * We reuse Facebook's own oracle X.2yR.A00(X.2nv):boolean — proven by
 * X.2yR.A01 to mean exactly "this is the Sponsored · Not connected to <friend>
 * unit". In the hook we:
 *   1. pull the X.2nv argument,
 *   2. call the real X.2yR.A00(env),
 *   3. if true, return null from X.2ug.A00 so the entire sponsored unit
 *      collapses — while every non-sponsored story unit builds normally.
 *
 * Because we only ever null the story-unit builder for units the oracle flags,
 * comments and other shared components are never affected.
 *
 * v6 news-feed edge skip retained.
 */

private const val LOG_TAG = "NexAlloy/HideProfileTimelineAds"

private val inFeedAssembly: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
private val sponsoredStories: MutableMap<Any, Boolean> =
    java.util.Collections.synchronizedMap(java.util.WeakHashMap())

private val droppedUnits = AtomicInteger(0)
private val logBudget = AtomicInteger(40)

val HideProfileTimelineAds = patch(
    name = "Hide profile-timeline ads",
    description = "Drops the 'Sponsored · Not connected to <friend>' unit at the story-unit builder (X.2ug.A00) using Facebook's own oracle.",
) {
    runCatching {
        val graphQLStory = classLoader.loadClass("com.facebook.graphql.model.GraphQLStory")
        val feedUnitEdge = classLoader.loadClass("com.facebook.graphql.model.GraphQLFeedUnitEdge")

        fun log(msg: String) {
            if (logBudget.getAndDecrement() > 0) XposedBridge.log("$LOG_TAG: $msg")
        }

        // Resolve the oracle X.2yR.A00(X.2nv):boolean and the X.2nv class.
        val env2nv = runCatching { classLoader.loadClass("X.2nv") }.getOrNull()
        val oracleA00: java.lang.reflect.Method? = runCatching {
            val c = classLoader.loadClass("X.2yR")
            c.declaredMethods.firstOrNull { m ->
                m.name == "A00" && m.returnType == java.lang.Boolean.TYPE &&
                    m.parameterCount == 1 &&
                    (env2nv == null || m.parameterTypes[0] == env2nv)
            }?.also { it.isAccessible = true }
        }.getOrNull()
        XposedBridge.log("$LOG_TAG: oracle X.2yR.A00 resolved=${oracleA00 != null}, X.2nv=${env2nv != null}")

        fun isSponsoredEnv(env: Any?): Boolean {
            if (env == null || oracleA00 == null) return false
            return runCatching { oracleA00.invoke(null, env) as? Boolean }.getOrNull() == true
        }

        // ── X.2ug.A00 — story-unit builder; return null for the ad unit ─────
        runCatching {
            val c2ug = classLoader.loadClass("X.2ug")
            val a00 = c2ug.declaredMethods.firstOrNull { m ->
                m.name == "A00" && Modifier.isStatic(m.modifiers) &&
                    !m.returnType.isPrimitive && m.returnType != Void.TYPE &&
                    // must take both an X.2nv and a GraphQLStory somewhere
                    m.parameterTypes.any { env2nv != null && it == env2nv } &&
                    m.parameterTypes.any { it == graphQLStory }
            }
            if (a00 != null) {
                a00.isAccessible = true
                // find the X.2nv parameter index
                val envIdx = a00.parameterTypes.indexOfFirst { env2nv != null && it == env2nv }
                XposedBridge.hookMethod(a00, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val env = if (envIdx >= 0) param.args.getOrNull(envIdx) else null
                        if (isSponsoredEnv(env)) {
                            // Collapse the whole sponsored story unit.
                            param.result = null
                            val n = droppedUnits.incrementAndGet()
                            if (n <= 10 || n % 20 == 0) {
                                XposedBridge.log("$LOG_TAG: dropped sponsored story-unit at X.2ug.A00 (total=$n)")
                            }
                        }
                    }
                })
                XposedBridge.log("$LOG_TAG: hooked X.2ug.A00 (story-unit builder, envIdx=$envIdx)")
            } else {
                XposedBridge.log("$LOG_TAG: WARN X.2ug.A00 not found with expected shape")
            }
        }.onFailure { log("2ug hook failed: ${it.message}") }

        // ── Retain v6 news-feed edge skip ───────────────────────────────────
        runCatching {
            val getSD = graphQLStory.declaredMethods.firstOrNull { m ->
                m.name == "A0N" && m.parameterCount == 0 && !Modifier.isStatic(m.modifiers)
            }?.also { it.isAccessible = true }
            if (getSD != null) {
                XposedBridge.hookMethod(getSD, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val s = param.thisObject
                        if (s != null && param.result != null) sponsoredStories[s] = true
                    }
                })
            }
            val edgeNodeGetters = feedUnitEdge.declaredMethods.filter { m ->
                m.parameterCount == 0 && !Modifier.isStatic(m.modifiers) &&
                    (m.name == "A03" || m.name == "BI0")
            }.onEach { it.isAccessible = true }
            fun edgeIsSponsored(edge: Any?): Boolean {
                if (edge == null || !feedUnitEdge.isInstance(edge)) return false
                for (g in edgeNodeGetters) {
                    val node = runCatching { g.invoke(edge) }.getOrNull() ?: continue
                    if (graphQLStory.isInstance(node) && sponsoredStories[node] == true) return true
                }
                return false
            }
            val feedConverter = classLoader.loadClass("X.2AL")
            for (m in feedConverter.declaredMethods) {
                if (m.name == "A03" || m.name == "A0G" || m.name.startsWith("convertViewerToHomeStories")) {
                    m.isAccessible = true
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) { inFeedAssembly.set(true) }
                        override fun afterHookedMethod(param: MethodHookParam) { inFeedAssembly.set(false) }
                    })
                }
            }
            val builderClass = classLoader.loadClass("com.google.common.collect.ImmutableList\$Builder")
            val addMethod = builderClass.getMethod("add", Any::class.java)
            XposedBridge.hookMethod(addMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (inFeedAssembly.get() != true) return
                    val arg = param.args.getOrNull(0) ?: return
                    if (edgeIsSponsored(arg)) param.result = param.thisObject
                }
            })
            XposedBridge.log("$LOG_TAG: news-feed skip retained")
        }.onFailure { log("news-feed hook failed: ${it.message}") }
    }.onFailure { e ->
        XposedBridge.log("$LOG_TAG: patch install failed: ${e.javaClass.name}: ${e.message}")
    }
}
