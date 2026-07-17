package io.github.nexalloy.revanced.facebook.profileads

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * v10 — cover BOTH profile-ad render roots with one shared ad-marker.
 *
 * v8 revealed the profile ad reaches X.2yR.A00 (the sponsored-unit oracle;
 * true ⟺ "Sponsored · Not connected to <friend>") through TWO independent
 * Litho render roots:
 *
 *   branch 1:  X.NpY.A18(CallerContext):3RU
 *                → NpY.AHy → 2yL.A17 → 2yL.A05 → 2wv.C0w → 2vF.A08 → 2yR.A01 → 2yR.A00
 *   branch 2:  X.2zd.render(240,25F):3RU   (2zd ⟶ NpX ⟶ 1Kr KComponent)
 *                → NpX.render → 2zd.render → 24M.A00 → 5IL.invoke → 2wv.C0w → 2vF.A08 → 2yR.A01 → 2yR.A00
 *
 * v9 only hooked branch 1 (NpY.A18); the "mụn lưng" ad happens to render via
 * branch 2, so nothing dropped. v10 hooks the render ROOT of BOTH branches
 * and shares a single ThreadLocal ad-marker driven by the real X.2yR.A00
 * result. Whichever root is on the stack when X.2yR.A00 returns true has its
 * rendered component replaced with null → the whole ad row collapses.
 *
 * Render-window semantics (per root invocation, per thread):
 *   root.before → renderDepth++ ; push marker=false for this frame
 *   2yR.A00.after → if renderDepth>0 && result==true → mark current frame ad
 *   root.after  → if this frame was marked ad → result=null ; renderDepth--
 *
 * A depth counter (not a bool) handles the case where roots nest. Only the
 * frame that actually contained the sponsored oracle result is dropped, so
 * ordinary posts are never touched.
 *
 * v6 news-feed edge skip retained.
 */

private const val LOG_TAG = "NexAlloy/HideProfileTimelineAds"

// Stack of per-render-frame "is ad" flags on this thread.
private val adFrameStack: ThreadLocal<ArrayDeque<Boolean>> =
    ThreadLocal.withInitial { ArrayDeque<Boolean>() }
private val inFeedAssembly: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
private val sponsoredStories: MutableMap<Any, Boolean> =
    java.util.Collections.synchronizedMap(java.util.WeakHashMap())

private val droppedRows = AtomicInteger(0)
private val logBudget = AtomicInteger(40)

private fun markCurrentFrameAsAd() {
    val st = adFrameStack.get()
    if (st.isNotEmpty()) {
        st.removeLast()
        st.addLast(true)
    }
}

val HideProfileTimelineAds = patch(
    name = "Hide profile-timeline ads",
    description = "Drops the whole 'Sponsored · Not connected to <friend>' profile-ad row across both Litho render roots.",
) {
    runCatching {
        val graphQLStory = classLoader.loadClass("com.facebook.graphql.model.GraphQLStory")
        val feedUnitEdge = classLoader.loadClass("com.facebook.graphql.model.GraphQLFeedUnitEdge")

        fun log(msg: String) {
            if (logBudget.getAndDecrement() > 0) XposedBridge.log("$LOG_TAG: $msg")
        }

        // Shared render-root hook installer.
        fun hookRenderRoot(className: String, methodName: String, expectedParams: Int) {
            runCatching {
                val cls = classLoader.loadClass(className)
                val methods = cls.declaredMethods.filter { m ->
                    m.name == methodName &&
                        !m.returnType.isPrimitive && m.returnType != Void.TYPE &&
                        (expectedParams < 0 || m.parameterCount == expectedParams)
                }
                if (methods.isEmpty()) {
                    XposedBridge.log("$LOG_TAG: WARN $className.$methodName not found")
                    return@runCatching
                }
                for (m in methods) {
                    m.isAccessible = true
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            adFrameStack.get().addLast(false)
                        }
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val st = adFrameStack.get()
                            val wasAd = if (st.isNotEmpty()) st.removeLast() else false
                            if (wasAd) {
                                param.result = null
                                val n = droppedRows.incrementAndGet()
                                if (n <= 10 || n % 20 == 0) {
                                    XposedBridge.log("$LOG_TAG: dropped ad row at $className.$methodName (total=$n)")
                                }
                            }
                        }
                    })
                    XposedBridge.log("$LOG_TAG: hooked render root $className.$methodName(${m.parameterCount})")
                }
            }.onFailure { log("$className.$methodName hook failed: ${it.message}") }
        }

        // ── X.2yR.A00 — the shared ad oracle ────────────────────────────────
        runCatching {
            val c2yR = classLoader.loadClass("X.2yR")
            val a00 = c2yR.declaredMethods.firstOrNull { m ->
                m.name == "A00" && m.returnType == java.lang.Boolean.TYPE
            }
            if (a00 != null) {
                a00.isAccessible = true
                XposedBridge.hookMethod(a00, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.result == true) markCurrentFrameAsAd()
                    }
                })
                XposedBridge.log("$LOG_TAG: hooked X.2yR.A00 (shared ad oracle)")
            } else {
                XposedBridge.log("$LOG_TAG: WARN X.2yR.A00 not found")
            }
        }.onFailure { log("2yR hook failed: ${it.message}") }

        // ── Both render roots ───────────────────────────────────────────────
        hookRenderRoot("X.NpY", "A18", 1)          // branch 1
        hookRenderRoot("X.2zd", "render", -1)      // branch 2 (render(240,25F))
        hookRenderRoot("X.NpX", "render", -1)      // branch 2 parent, belt-and-suspenders

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
