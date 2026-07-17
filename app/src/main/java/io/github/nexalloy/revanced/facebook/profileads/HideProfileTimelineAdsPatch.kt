package io.github.nexalloy.revanced.facebook.profileads

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * v7 — precise "not connected to <friend>" detection + pipeline capture.
 *
 * v6 skipped sponsored edges inside X.2AL (the NEWS-FEED pipeline) and the
 * log confirmed skips — but the profile-timeline ad still showed. Conclusion:
 * the "Sponsored · Not connected to <friend>" unit does NOT flow through the
 * X.2AL news-feed converter at all. It's a different surface.
 *
 * Two things changed here:
 *
 * 1. PRECISE DETECTION. Facebook marks this exact ad variant with the
 *    GraphQL boolean `is_non_connected_page_post` (field-name-hash
 *    -2074334701) inside the story's sponsoredData tree. We now detect it
 *    exactly instead of guessing:
 *        story.A0N()                       -> sponsoredData tree (LX/3yW;)
 *        NpI.A0p(tree, -2074334701)        -> Boolean is_non_connected_page_post
 *    This is the very check X.Qx1 / X.QXL use internally for profile ads.
 *
 * 2. WE NO LONGER NULL A0N. Nulling getSponsoredData blinded Facebook's own
 *    profile-ad detectors (X.2yR.A00, X.Qx1.A01 both read A0N), which likely
 *    corrupted the render path without removing the row. Instead we leave the
 *    data intact and remove the ad at its real chokepoint.
 *
 * 3. CAPTURE. For the first few non-connected profile ads seen, we log a full
 *    stack trace from inside A0N so we can see the exact profile-timeline
 *    render/convert method to neutralise. (The previous stack sampling fired
 *    on ALL sponsored stories — mostly news-feed noise. This one fires ONLY
 *    on the non-connected profile ad, so the trace will point straight at the
 *    profile pipeline.)
 *
 * The X.2AL news-feed edge filter from v6 is retained (it correctly hides
 * sponsored posts in the Home feed).
 *
 * Please send the next log: the "PROFILE-AD stack" lines identify the method
 * we hook in v8 to finally drop the row.
 */

private const val LOG_TAG = "NexAlloy/HideProfileTimelineAds"
private const val NON_CONNECTED_HASH = -2074334701

private val sponsoredStories: MutableMap<Any, Boolean> =
    java.util.Collections.synchronizedMap(java.util.WeakHashMap())
private val inFeedAssembly: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

private val profileAdStacksLeft = AtomicInteger(6)
private val newsFeedSkipped = AtomicInteger(0)
private val logBudget = AtomicInteger(20)

val HideProfileTimelineAds = patch(
    name = "Hide profile-timeline ads",
    description = "Detects 'Sponsored · Not connected to <friend>' profile ads precisely and captures their render pipeline; hides news-feed sponsored posts.",
) {
    runCatching {
        val graphQLStory = classLoader.loadClass("com.facebook.graphql.model.GraphQLStory")
        val feedUnitEdge = classLoader.loadClass("com.facebook.graphql.model.GraphQLFeedUnitEdge")
        val baseTree = classLoader.loadClass("com.facebook.graphql.modelutil.BaseModelWithTree")

        fun log(msg: String) {
            if (logBudget.getAndDecrement() > 0) XposedBridge.log("$LOG_TAG: $msg")
        }

        // NpI.A0p(tree, hash) -> Boolean  (reads is_non_connected_page_post)
        val npiA0p: java.lang.reflect.Method? = runCatching {
            classLoader.loadClass("X.NpI").declaredMethods.firstOrNull { m ->
                m.name == "A0p" && Modifier.isStatic(m.modifiers) &&
                    m.parameterCount == 2 &&
                    m.parameterTypes[0] == baseTree &&
                    m.parameterTypes[1] == Integer.TYPE &&
                    m.returnType == java.lang.Boolean::class.java
            }?.also { it.isAccessible = true }
        }.getOrNull()
        XposedBridge.log("$LOG_TAG: NpI.A0p resolved=${npiA0p != null}")

        val getSponsoredData = graphQLStory.declaredMethods.firstOrNull { m ->
            m.name == "A0N" && m.parameterCount == 0 &&
                !Modifier.isStatic(m.modifiers) && !m.isSynthetic
        }
        getSponsoredData?.isAccessible = true

        fun isNonConnectedProfileAd(story: Any?): Boolean {
            if (story == null || getSponsoredData == null || npiA0p == null) return false
            val tree = runCatching { getSponsoredData.invoke(story) }.getOrNull() ?: return false
            if (!baseTree.isInstance(tree)) return false
            val b = runCatching { npiA0p.invoke(null, tree, NON_CONNECTED_HASH) as? Boolean }.getOrNull()
            return b == true
        }

        // ── 1. A0N — DETECT (do NOT null). Record identity; capture stack for
        //         non-connected profile ads only.
        if (getSponsoredData != null) {
            XposedBridge.hookMethod(getSponsoredData, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val story = param.thisObject ?: return
                    val tree = param.result ?: return
                    // mark general sponsored identity (for news-feed filter)
                    sponsoredStories[story] = true
                    // precise non-connected detection for stack capture
                    if (baseTree.isInstance(tree)) {
                        val nc = runCatching {
                            npiA0p?.invoke(null, tree, NON_CONNECTED_HASH) as? Boolean
                        }.getOrNull() == true
                        if (nc && profileAdStacksLeft.getAndDecrement() > 0) {
                            val st = Thread.currentThread().stackTrace
                                .drop(3).take(18)
                                .joinToString(" <- ") { "${it.className}.${it.methodName}" }
                            XposedBridge.log("$LOG_TAG: PROFILE-AD stack: $st")
                        }
                    }
                    // NOTE: result left intact on purpose (see header).
                }
            })
            XposedBridge.log("$LOG_TAG: hooked GraphQLStory.A0N() (detect + capture, no null)")
        } else {
            XposedBridge.log("$LOG_TAG: WARN A0N not found")
        }

        // ── 2. News-feed edge skip (retained from v6) ────────────────────────
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

        val feedConverter = runCatching { classLoader.loadClass("X.2AL") }.getOrNull()
        if (feedConverter != null) {
            var wh = 0
            for (m in feedConverter.declaredMethods) {
                val isAssembly = m.name == "A03" || m.name == "A0G" ||
                    m.name.startsWith("convertViewerToHomeStories")
                if (!isAssembly) continue
                m.isAccessible = true
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) { inFeedAssembly.set(true) }
                    override fun afterHookedMethod(param: MethodHookParam) { inFeedAssembly.set(false) }
                })
                wh++
            }
            XposedBridge.log("$LOG_TAG: news-feed window hooks=$wh")

            runCatching {
                val builderClass = classLoader.loadClass("com.google.common.collect.ImmutableList\$Builder")
                val addMethod = builderClass.getMethod("add", Any::class.java)
                XposedBridge.hookMethod(addMethod, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (inFeedAssembly.get() != true) return
                        val arg = param.args.getOrNull(0) ?: return
                        if (edgeIsSponsored(arg)) {
                            param.result = param.thisObject
                            newsFeedSkipped.incrementAndGet()
                        }
                    }
                })
                XposedBridge.log("$LOG_TAG: hooked ImmutableList\$Builder.add (news-feed skip)")
            }
        }
    }.onFailure { e ->
        XposedBridge.log("$LOG_TAG: patch install failed: ${e.javaClass.name}: ${e.message}")
    }
}
