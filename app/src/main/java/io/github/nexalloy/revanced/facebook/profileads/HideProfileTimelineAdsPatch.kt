package io.github.nexalloy.revanced.facebook.profileads

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * v9 — drop the whole profile-ad row at its Litho render root (X.NpY.A18).
 *
 * v8's stack trace nailed the profile-ad pipeline:
 *
 *   X.NpY.A18(CallerContext):3RU        ← render ROOT of the profile story unit
 *     └ X.NpY.AHy(26U):3XA              build layout
 *         └ X.2ug.A1G / X.2yL.A17 …     child components
 *             └ X.2yR.A00(2nv):Z        "is this a sponsored profile unit?"
 *
 * And X.2yR.A01 proved the semantics:
 *     if (X.2yR.A00(env)) buildSponsoredLabel() else return "";
 * so X.2yR.A00 == true  ⟺  this unit is the "Sponsored · Not connected to
 * <friend>" ad. (v8 forcing QOr.A05 / nulling QXL never fired — those are a
 * different CTA path — which is why only [2yR.A00] stacks appeared.)
 *
 * Mechanism (ThreadLocal render window):
 *   1. X.NpY.A18  beforeHooked → push a fresh per-call flag (currentUnitIsAd
 *      = false) and remember we're inside a render.
 *   2. X.2yR.A00  afterHooked → if it returned true while we're inside an
 *      NpY.A18 render on this thread, set currentUnitIsAd = true. (We read the
 *      REAL return value, so genuine non-ad stories are never affected.)
 *   3. X.NpY.A18  afterHooked → if currentUnitIsAd, replace the rendered
 *      component with null. In Litho a null child collapses to zero size, so
 *      the entire ad row — image, caption, Shopee links, like/comment bar,
 *      and the "Được tài trợ · Chưa kết nối với" header — disappears.
 *
 * Because the flag is per-NpY.A18-invocation and gated by the real X.2yR.A00
 * result, only the sponsored profile unit is dropped; ordinary posts on the
 * same timeline render normally.
 *
 * The v6 news-feed edge skip is retained unchanged.
 */

private const val LOG_TAG = "NexAlloy/HideProfileTimelineAds"

private val insideNpyRender: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
private val currentUnitIsAd: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
private val inFeedAssembly: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
private val sponsoredStories: MutableMap<Any, Boolean> =
    java.util.Collections.synchronizedMap(java.util.WeakHashMap())

private val droppedRows = AtomicInteger(0)
private val logBudget = AtomicInteger(30)

val HideProfileTimelineAds = patch(
    name = "Hide profile-timeline ads",
    description = "Drops the entire 'Sponsored · Not connected to <friend>' profile-ad row at its Litho render root.",
) {
    runCatching {
        val graphQLStory = classLoader.loadClass("com.facebook.graphql.model.GraphQLStory")
        val feedUnitEdge = classLoader.loadClass("com.facebook.graphql.model.GraphQLFeedUnitEdge")

        fun log(msg: String) {
            if (logBudget.getAndDecrement() > 0) XposedBridge.log("$LOG_TAG: $msg")
        }

        // ── X.2yR.A00(2nv):Z — mark the current NpY render as an ad ─────────
        runCatching {
            val c2yR = classLoader.loadClass("X.2yR")
            val a00 = c2yR.declaredMethods.firstOrNull { m ->
                m.name == "A00" && m.returnType == java.lang.Boolean.TYPE &&
                    Modifier.isStatic(m.modifiers) && m.parameterCount == 1
            } ?: c2yR.declaredMethods.firstOrNull { m ->
                m.name == "A00" && m.returnType == java.lang.Boolean.TYPE
            }
            if (a00 != null) {
                a00.isAccessible = true
                XposedBridge.hookMethod(a00, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (insideNpyRender.get() == true && param.result == true) {
                            currentUnitIsAd.set(true)
                        }
                    }
                })
                XposedBridge.log("$LOG_TAG: hooked X.2yR.A00 (ad marker)")
            } else {
                XposedBridge.log("$LOG_TAG: WARN X.2yR.A00 not found")
            }
        }.onFailure { log("2yR hook failed: ${it.message}") }

        // ── X.NpY.A18(CallerContext):3RU — render root, drop if ad ──────────
        runCatching {
            val npy = classLoader.loadClass("X.NpY")
            val a18 = npy.declaredMethods.firstOrNull { m ->
                m.name == "A18" && m.parameterCount == 1 &&
                    !m.returnType.isPrimitive && m.returnType != Void.TYPE
            }
            if (a18 != null) {
                a18.isAccessible = true
                XposedBridge.hookMethod(a18, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        insideNpyRender.set(true)
                        currentUnitIsAd.set(false)
                    }
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val wasAd = currentUnitIsAd.get() == true
                        insideNpyRender.set(false)
                        currentUnitIsAd.set(false)
                        if (wasAd) {
                            param.result = null
                            val n = droppedRows.incrementAndGet()
                            if (n <= 8 || n % 20 == 0) {
                                XposedBridge.log("$LOG_TAG: dropped profile-ad row at NpY.A18 (total=$n)")
                            }
                        }
                    }
                })
                XposedBridge.log("$LOG_TAG: hooked X.NpY.A18 (render-root drop)")
            } else {
                XposedBridge.log("$LOG_TAG: WARN X.NpY.A18 not found")
            }
        }.onFailure { log("NpY hook failed: ${it.message}") }

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
