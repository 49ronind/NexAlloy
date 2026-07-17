package io.github.nexalloy.revanced.facebook.profileads

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * v5 — filter the ad OUT OF THE FEED LIST at the data-assembly chokepoint.
 *
 * The v4 stack trace proved the ad never reaches the timeline RENDER spec
 * (X.9c8). Instead the sponsored check runs deep inside feed-list assembly:
 *
 *     X.2AL.A0G(...)                         public feed-conversion entry
 *       └─ X.2AL.A03(...)  (1411 instrs)     the real loop
 *            ├─ per edge: build X/2bP props
 *            │    └─ X.2o6.A02(story)        isSponsoredStory  ← our A0N fires here
 *            ├─ ImmutableList$Builder.add(edge)   ← [1155] edge is appended
 *            └─ new X.3CC(... , edgeList=A05, ...) ← result holder
 *
 * X.2AL.A03 appends EVERY converted edge to the output list regardless of
 * the sponsored flag — that is why nulling getSponsoredData only removed the
 * label, not the row. The fix is to drop sponsored edges from the resulting
 * list.
 *
 * We hook the PUBLIC entry X.2AL.A0G (and, as a fallback, the Kotlin-named
 * convertViewerToHomeStories$... method) and post-process its X.3CC result:
 *   - field A05 (the first ImmutableList ctor arg, index 6) is the
 *     ImmutableList<GraphQLFeedUnitEdge> of stories to display.
 *   - for each edge, unwrap the node (edge.A03()/BI0()) to a GraphQLStory and
 *     ask the ORIGINAL X.2o6.A02(story) whether it is sponsored. Because the
 *     A0N (getSponsoredData) hook has ALREADY recorded every sponsored story's
 *     identity into `sponsoredStories` during A03's run, we can also fall back
 *     to identity lookup even after A0N has been nulled.
 *   - rebuild the ImmutableList without sponsored edges and write it back to
 *     A05 via reflection.
 *
 * Net effect: the "Sponsored · Not connected to <friend>" unit is gone from
 * the profile feed entirely — no label, no body, no row.
 *
 * Diagnostics: logs each install; logs the first few filters with before/after
 * counts.
 */

private const val LOG_TAG = "NexAlloy/HideProfileTimelineAds"

private val sponsoredStories: MutableMap<Any, Boolean> =
    java.util.Collections.synchronizedMap(java.util.WeakHashMap())

private val filteredCount = AtomicInteger(0)
private val logSamplesLeft = AtomicInteger(8)

val HideProfileTimelineAds = patch(
    name = "Hide profile-timeline ads",
    description = "Removes 'Sponsored · Not connected to <friend>' ads by filtering sponsored edges out of the profile/feed story list.",
) {
    runCatching {
        val graphQLStory = classLoader.loadClass("com.facebook.graphql.model.GraphQLStory")
        val feedUnitEdge = classLoader.loadClass("com.facebook.graphql.model.GraphQLFeedUnitEdge")

        // ── 1. A0N — record identity of every sponsored story, then null it ──
        val getSponsoredData = graphQLStory.declaredMethods.firstOrNull { m ->
            m.name == "A0N" && m.parameterCount == 0 &&
                !Modifier.isStatic(m.modifiers) && !m.isSynthetic
        }
        if (getSponsoredData != null) {
            getSponsoredData.isAccessible = true
            XposedBridge.hookMethod(getSponsoredData, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val story = param.thisObject
                    if (story != null && param.result != null) {
                        sponsoredStories[story] = true
                    }
                    param.result = null
                }
            })
            XposedBridge.log("$LOG_TAG: hooked GraphQLStory.A0N() (record identity + null)")
        } else {
            XposedBridge.log("$LOG_TAG: WARN — GraphQLStory.A0N() not resolved")
        }

        // ── 2. Resolve the original isSponsoredStory oracle X.2o6.A02 ────────
        // We call it reflectively during filtering. NOTE: it internally calls
        // A0N which we've nulled, so it will now say "not sponsored". That's
        // why we ALSO keep the identity map as the primary signal and use A02
        // only as a secondary hint (for stories seen before our hook, etc.).
        val isSponsoredStoryMethod: java.lang.reflect.Method? = runCatching {
            val c = classLoader.loadClass("X.2o6")
            c.declaredMethods.firstOrNull { m ->
                Modifier.isStatic(m.modifiers) && m.parameterCount == 1 &&
                    m.parameterTypes[0] == graphQLStory &&
                    m.returnType == java.lang.Boolean.TYPE
            }?.also { it.isAccessible = true }
        }.getOrNull()

        // node-unwrap getters on the edge: A03() and BI0() both return LX/3Jv;
        val edgeNodeGetters = feedUnitEdge.declaredMethods.filter { m ->
            m.parameterCount == 0 && !Modifier.isStatic(m.modifiers) &&
                (m.name == "A03" || m.name == "BI0")
        }.onEach { it.isAccessible = true }

        fun edgeIsSponsored(edge: Any?): Boolean {
            if (edge == null) return false
            for (g in edgeNodeGetters) {
                val node = runCatching { g.invoke(edge) }.getOrNull() ?: continue
                if (graphQLStory.isInstance(node)) {
                    // primary: identity recorded during A0N
                    if (sponsoredStories[node] == true) return true
                    // secondary: ask the oracle (may be null-blinded, best-effort)
                    val viaOracle = isSponsoredStoryMethod?.let { mm ->
                        runCatching { mm.invoke(null, node) as? Boolean }.getOrNull()
                    } ?: false
                    if (viaOracle) return true
                }
            }
            return false
        }

        // ── 3. Hook X.2AL feed-conversion methods, filter A05 edge list ─────
        val feedConverter = classLoader.loadClass("X.2AL")

        // The ImmutableList<GraphQLFeedUnitEdge> of display stories is ctor
        // param 7, which maps to field A06 (verified from X.3CC.<init>). The
        // other two ImmutableList fields (A05, A09) hold different data; we
        // scan all three defensively — a list with no GraphQLFeedUnitEdge
        // elements yields zero matches from edgeIsSponsored() and is left
        // untouched, so this is safe even if the field mapping shifts.
        val resultHolder = classLoader.loadClass("X.3CC")
        val edgeListFields = listOf("A05", "A06", "A09").mapNotNull { fn ->
            runCatching { resultHolder.getDeclaredField(fn).also { it.isAccessible = true } }.getOrNull()
        }
        if (edgeListFields.isEmpty()) {
            XposedBridge.log("$LOG_TAG: WARN — no X.3CC ImmutableList fields found; cannot filter")
        }

        val immutableList = classLoader.loadClass("com.google.common.collect.ImmutableList")
        val copyOf = immutableList.getMethod("copyOf", java.lang.Iterable::class.java)

        val filterResult = fn@{ result: Any? ->
            if (result == null) return@fn
            if (!resultHolder.isInstance(result)) return@fn
            for (field in edgeListFields) {
                val list = runCatching { field.get(result) as? List<*> }.getOrNull() ?: continue
                if (list.isEmpty()) continue
                // quick type gate: only touch lists that actually contain edges
                if (!feedUnitEdge.isInstance(list.first())) continue
                val kept = ArrayList<Any?>(list.size)
                var removed = 0
                for (edge in list) {
                    if (edgeIsSponsored(edge)) removed++ else kept.add(edge)
                }
                if (removed > 0) {
                    val newList = copyOf.invoke(null, kept)
                    field.set(result, newList)
                    val total = filteredCount.addAndGet(removed)
                    if (logSamplesLeft.getAndDecrement() > 0) {
                        XposedBridge.log("$LOG_TAG: filtered $removed sponsored edge(s) from ${field.name} " +
                            "(${list.size} -> ${kept.size}, total=$total)")
                    }
                }
            }
        }

        var hooks = 0
        for (m in feedConverter.declaredMethods) {
            if (m.returnType != resultHolder) continue
            // A0G(...)LX/3CC;  and  convertViewerToHomeStories$...(...)LX/3CC;
            m.isAccessible = true
            XposedBridge.hookMethod(m, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    runCatching { filterResult(param.result) }
                }
            })
            hooks++
            XposedBridge.log("$LOG_TAG: hooked X.2AL.${m.name}(...)->X.3CC (feed-list filter)")
        }
        if (hooks == 0) {
            XposedBridge.log("$LOG_TAG: WARN — no X.2AL method returning X.3CC was hooked")
        }
    }.onFailure { e ->
        XposedBridge.log("$LOG_TAG: patch install failed: ${e.javaClass.name}: ${e.message}")
    }
}
