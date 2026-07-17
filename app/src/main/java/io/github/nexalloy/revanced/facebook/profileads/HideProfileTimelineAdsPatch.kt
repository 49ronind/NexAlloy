package io.github.nexalloy.revanced.facebook.profileads

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * v8 — hook the profile-ad classifier cluster directly + capture + suppress.
 *
 * v7 got no PROFILE-AD stack lines even though the ad was on screen, so the
 * ad's sponsoredData tree does NOT carry is_non_connected_page_post the way I
 * assumed — my A0N-based detector was in the wrong place.
 *
 * Static analysis of the profile-ad cluster (all read the story out of an
 * X.2nv "environment" wrapper, field A01) gives the real chokepoints:
 *
 *   X.Qx1.A00(X.2nv) : X.QOr      classify a profile story into a profile-ad
 *                                  CTA kind enum (A02..A0A). Returns A05/A02
 *                                  for the ad variants 2yR keys on.
 *   X.Qx1.A01(GraphQLStory) : X.QOr   same, story-typed overload; reads
 *                                  is_non_connected_page_post via NpI.A0p.
 *   X.QXL.A00(GraphQLStory) : X.3yW   returns the profile-ad action-link tree
 *                                  when the story is a non-connected page post.
 *   X.2yR.A00(X.2nv) : boolean    "should this profile story render as a
 *                                  normal unit?" — the gate above the row.
 *   X.RJS.AvC(...) : X.326        profile-ad component render entry.
 *
 * v8 does three things at each of these:
 *   (a) CAPTURE: log a stack trace the first few times each fires, so we can
 *       see the true render caller regardless of which one the ad hits.
 *   (b) SUPPRESS (best-effort, safe):
 *         - Qx1.A00 / Qx1.A01  -> force return QOr.A05 (the "not an ad / no
 *           CTA" sentinel that 2yR.A00 treats as non-ad at instr [39-40]).
 *         - QXL.A00            -> return null (no profile-ad action link ->
 *           the sponsored CTA/label path is skipped).
 *         - RJS.AvC            -> return null (empty component for the ad's
 *           attachment render).
 *   (c) Keep the v6 news-feed edge skip.
 *
 * If forcing QOr.A05 removes the row, we're done. If it only removes the CTA
 * but leaves the post, the captured RJS.AvC/stack trace tells us the row-level
 * spec to null in v9.
 */

private const val LOG_TAG = "NexAlloy/HideProfileTimelineAds"

private val inFeedAssembly: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
private val sponsoredStories: MutableMap<Any, Boolean> =
    java.util.Collections.synchronizedMap(java.util.WeakHashMap())
private val stackBudget = java.util.concurrent.ConcurrentHashMap<String, AtomicInteger>()
private val logBudget = AtomicInteger(40)

val HideProfileTimelineAds = patch(
    name = "Hide profile-timeline ads",
    description = "Suppresses 'Sponsored · Not connected to <friend>' profile ads via the profile-ad classifier cluster.",
) {
    runCatching {
        val graphQLStory = classLoader.loadClass("com.facebook.graphql.model.GraphQLStory")
        val feedUnitEdge = classLoader.loadClass("com.facebook.graphql.model.GraphQLFeedUnitEdge")

        fun log(msg: String) {
            if (logBudget.getAndDecrement() > 0) XposedBridge.log("$LOG_TAG: $msg")
        }
        fun captureStack(tag: String) {
            val b = stackBudget.getOrPut(tag) { AtomicInteger(3) }
            if (b.getAndDecrement() <= 0) return
            val st = Thread.currentThread().stackTrace.drop(3).take(20)
                .joinToString(" <- ") { "${it.className}.${it.methodName}" }
            XposedBridge.log("$LOG_TAG: [$tag] stack: $st")
        }

        // Resolve QOr enum + its A05 sentinel (the "no CTA / not an ad" value).
        val qorClass = runCatching { classLoader.loadClass("X.QOr") }.getOrNull()
        val qorA05 = runCatching {
            qorClass?.getDeclaredField("A05")?.also { it.isAccessible = true }?.get(null)
        }.getOrNull()
        val qorA02 = runCatching {
            qorClass?.getDeclaredField("A02")?.also { it.isAccessible = true }?.get(null)
        }.getOrNull()
        XposedBridge.log("$LOG_TAG: QOr.A05 resolved=${qorA05 != null}")

        // ── Hook X.Qx1.A00(2nv):QOr and A01(GraphQLStory):QOr → force A05 ───
        runCatching {
            val qx1 = classLoader.loadClass("X.Qx1")
            for (m in qx1.declaredMethods) {
                if (qorClass != null && m.returnType == qorClass &&
                    (m.name == "A00" || m.name == "A01")) {
                    m.isAccessible = true
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val r = param.result
                            // Only override when it classified as an ad kind
                            // (anything other than the A05 sentinel).
                            if (r != null && r !== qorA05) {
                                captureStack("Qx1.${m.name}")
                                if (qorA05 != null) param.result = qorA05
                            }
                        }
                    })
                    XposedBridge.log("$LOG_TAG: hooked X.Qx1.${m.name} -> force QOr.A05")
                }
            }
        }.onFailure { log("Qx1 hook failed: ${it.message}") }

        // ── Hook X.QXL.A00(GraphQLStory):3yW → null (drop profile action link)
        runCatching {
            val qxl = classLoader.loadClass("X.QXL")
            for (m in qxl.declaredMethods) {
                if (m.name == "A00" && m.parameterCount == 1 &&
                    m.parameterTypes[0] == graphQLStory) {
                    m.isAccessible = true
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (param.result != null) {
                                captureStack("QXL.A00")
                                param.result = null
                            }
                        }
                    })
                    XposedBridge.log("$LOG_TAG: hooked X.QXL.A00 -> null")
                }
            }
        }.onFailure { log("QXL hook failed: ${it.message}") }

        // ── Hook X.2yR.A00(2nv):Z → capture only (gate; don't flip yet) ─────
        runCatching {
            val c2yR = classLoader.loadClass("X.2yR")
            for (m in c2yR.declaredMethods) {
                if (m.name == "A00" && m.returnType == java.lang.Boolean.TYPE) {
                    m.isAccessible = true
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            captureStack("2yR.A00")
                        }
                    })
                    XposedBridge.log("$LOG_TAG: hooked X.2yR.A00 (capture)")
                }
            }
        }.onFailure { log("2yR hook failed: ${it.message}") }

        // ── Hook X.RJS.AvC(...):X.326 → capture + null (profile-ad render) ──
        runCatching {
            val rjs = classLoader.loadClass("X.RJS")
            for (m in rjs.declaredMethods) {
                if (m.name == "AvC") {
                    m.isAccessible = true
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            captureStack("RJS.AvC")
                            // Do NOT null yet — capture first; nulling a shared
                            // attachment renderer may blank non-ad attachments.
                        }
                    })
                    XposedBridge.log("$LOG_TAG: hooked X.RJS.AvC (capture)")
                }
            }
        }.onFailure { log("RJS hook failed: ${it.message}") }

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
                        // no null — keep FB detectors working
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
