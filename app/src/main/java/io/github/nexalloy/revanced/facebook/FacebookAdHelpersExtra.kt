package io.github.nexalloy.revanced.facebook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Method
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

private val extraMethodsHooked =
    Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

private fun once(method: Method): Boolean = extraMethodsHooked.add(methodHookKey(method))

private fun emptyImmutableList(classLoader: ClassLoader): Any? = runCatching {
    Class.forName("com.google.common.collect.ImmutableList", false, classLoader)
        .getDeclaredMethod("of")
        .invoke(null)
}.getOrNull()

fun hookReelsInstreamAdBreakParser(method: Method, classLoader: ClassLoader): Boolean {
    if (!once(method)) return false
    val empty = emptyImmutableList(classLoader) ?: return false
    method.isAccessible = true
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            logHookHitThrottled("reelsInstreamAdBreakParser", method)
            param.result = empty
        }
    })
    return true
}
