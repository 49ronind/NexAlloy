package io.github.nexalloy.revanced.facebook.profileads

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * v6 — intercept the edge at ImmutableList$Builder.add() INSIDE X.2AL.A03,
 * so the sponsored edge never enters the story list in the first place.
 *
 * Why v5 wasn't enough: v5 filtered X.3CC.A06 in A0G/A03 afterHook and the
 * log proved it worked ("filtered ... A06 (1 -> 0)"), yet the ad still showed.
 * That means a consumer captures the edges DURING A03's run — before the
 * afterHook rewrite — most likely via the X.2N1.onModelUpdate model-observer
 * that sits directly above A03 in the stack. Post-hoc list rewriting loses
 * that race.
 *
 * v6 removes the race by dropping the edge at the exact append site:
 *
 *     X.2AL.A03:
 *        [258] new ImmutableList$Builder            (v24)
 *        ...
 *        [1148] edge = X.2bP.A03(Z)                 (build finalized edge)
 *        [1155] v24.add(edge)                       ← we intercept HERE
 *        ...
 *        [1235] list = X.3XE.A03(v24)
 *
 * Mechanism:
 *   1. Hook GraphQLStory.A0N(): record sponsored-story identity, return null
 *      (kills label + impression everywhere).
 *   2. Hook X.2AL.A03 + A0G + convertViewerToHomeStories: mark a ThreadLocal
 *      "inFeedAssembly" window for the duration of the call.
 *   3. Hook com.google.common.collect.ImmutableList$Builder.add(Object):
 *      when inside the feed-assembly window AND the argument is a
 *      GraphQLFeedUnitEdge whose node is a sponsored GraphQLStory, SKIP the
 *      add (return the builder unchanged) so the edge is never listed.
 *   4. Keep the v5 afterHook list-filter as a belt-and-suspenders second pass.
 *
 * Because the Builder.add hook is gated by the feed-assembly ThreadLocal, it
 * never touches unrelated ImmutableList building elsewhere in the app.
 */

private const val LOG_TAG = "NexAlloy/HideProfileTimelineAds"

private val sponsoredStories: MutableMap<Any, Boolean> =
    java.util.Collections.synchronizedMap(java.util.WeakHashMap())

private val inFeedAssembly: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

private val skippedAtAdd = AtomicInteger(0)
private val filteredPostHoc = AtomicInteger(0)
private val logBudget = AtomicInteger(12)

val HideProfileTimelineAds = patch(
    name = "Hide profile-timeline ads",
    description = "Removes 'Sponsored · Not connected to <friend>' ads by skipping sponsored edges at the feed-list append site.",
) {
    runCatching {
        val graphQLStory = classLoader.loadClass("com.facebook.graphql.model.GraphQLStory")
        val feedUnitEdge = classLoader.loadClass("com.facebook.graphql.model.GraphQLFeedUnitEdge")

        fun log(msg: String) {
            if (logBudget.getAndDecrement() > 0) XposedBridge.log("$LOG_TAG: $msg")
        }

        // ── 1. A0N — record identity, null out ───────────────────────────────
        graphQLStory.declaredMethods.firstOrNull { m ->
            m.name == "A0N" && m.parameterCount == 0 &&
                !Modifier.isStatic(m.modifiers) && !m.isSynthetic
        }?.let { m ->
            m.isAccessible = true
            XposedBridge.hookMethod(m, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val story = param.thisObject
                    if (story != null && param.result != null) sponsoredStories[story] = true
                    param.result = null
                }
            })
            XposedBridge.log("$LOG_TAG: hooked GraphQLStory.A0N() (record + null)")
        } ?: XposedBridge.log("$LOG_TAG: WARN A0N not found")

        // Edge → node getters
        val edgeNodeGetters = feedUnitEdge.declaredMethods.filter { m ->
            m.parameterCount == 0 && !Modifier.isStatic(m.modifiers) &&
                (m.name == "A03" || m.name == "BI0")
        }.onEach { it.isAccessible = true }

        // isSponsoredStory oracle (secondary; A0N-blinded but harmless)
        val oracle = runCatching {
            classLoader.loadClass("X.2o6").declaredMethods.firstOrNull { m ->
                Modifier.isStatic(m.modifiers) && m.parameterCount == 1 &&
                    m.parameterTypes[0] == graphQLStory &&
                    m.returnType == java.lang.Boolean.TYPE
            }?.also { it.isAccessible = true }
        }.getOrNull()

        fun edgeIsSponsored(edge: Any?): Boolean {
            if (edge == null || !feedUnitEdge.isInstance(edge)) return false
            for (g in edgeNodeGetters) {
                val node = runCatching { g.invoke(edge) }.getOrNull() ?: continue
                if (graphQLStory.isInstance(node)) {
                    if (sponsoredStories[node] == true) return true
                    val viaOracle = oracle?.let { mm ->
                        runCatching { mm.invoke(null, node) as? Boolean }.getOrNull()
                    } ?: false
                    if (viaOracle) return true
                }
            }
            return false
        }

        // ── 2. Feed-assembly window on X.2AL.A03 / A0G / convert... ─────────
        val feedConverter = classLoader.loadClass("X.2AL")
        val resultHolder = classLoader.loadClass("X.3CC")
        val edgeListFields = listOf("A05", "A06", "A09").mapNotNull { fn ->
            runCatching { resultHolder.getDeclaredField(fn).also { it.isAccessible = true } }.getOrNull()
        }
        val immutableList = classLoader.loadClass("com.google.common.collect.ImmutableList")
        val copyOf = immutableList.getMethod("copyOf", java.lang.Iterable::class.java)

        fun postHocFilter(result: Any?) {
            if (result == null || !resultHolder.isInstance(result)) return
            for (field in edgeListFields) {
                val list = runCatching { field.get(result) as? List<*> }.getOrNull() ?: continue
                if (list.isEmpty() || !feedUnitEdge.isInstance(list.first())) continue
                val kept = ArrayList<Any?>(list.size)
                var removed = 0
                for (edge in list) if (edgeIsSponsored(edge)) removed++ else kept.add(edge)
                if (removed > 0) {
                    field.set(result, copyOf.invoke(null, kept))
                    log("post-hoc filtered $removed from ${field.name} (total=${filteredPostHoc.addAndGet(removed)})")
                }
            }
        }

        var windowHooks = 0
        for (m in feedConverter.declaredMethods) {
            val isAssembly = m.name == "A03" || m.name == "A0G" ||
                m.name.startsWith("convertViewerToHomeStories")
            if (!isAssembly) continue
            m.isAccessible = true
            XposedBridge.hookMethod(m, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    inFeedAssembly.set(true)
                }
                override fun afterHookedMethod(param: MethodHookParam) {
                    inFeedAssembly.set(false)
                    if (resultHolder.isInstance(param.result)) {
                        runCatching { postHocFilter(param.result) }
                    }
                }
            })
            windowHooks++
            XposedBridge.log("$LOG_TAG: window-hooked X.2AL.${m.name}")
        }
        if (windowHooks == 0) XposedBridge.log("$LOG_TAG: WARN no X.2AL assembly method hooked")

        // ── 3. ImmutableList$Builder.add — skip sponsored edges in-window ────
        val builderClass = classLoader.loadClass("com.google.common.collect.ImmutableList\$Builder")
        val addMethod = builderClass.getMethod("add", Any::class.java)
        XposedBridge.hookMethod(addMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (inFeedAssembly.get() != true) return
                val arg = param.args.getOrNull(0) ?: return
                if (edgeIsSponsored(arg)) {
                    // Skip the add: return the builder itself (add() returns the
                    // builder for chaining) without appending anything.
                    param.result = param.thisObject
                    val n = skippedAtAdd.incrementAndGet()
                    if (logBudget.get() > 0) log("skipped sponsored edge at Builder.add (total=$n)")
                }
            }
        })
        XposedBridge.log("$LOG_TAG: hooked ImmutableList\$Builder.add (in-window skip)")
    }.onFailure { e ->
        XposedBridge.log("$LOG_TAG: patch install failed: ${e.javaClass.name}: ${e.message}")
    }
}
