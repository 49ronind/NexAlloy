package io.github.nexalloy.revanced.spotify.premium

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

private data class OverrideAttr(val key: String, val value: Any, val expected: Boolean = true)

private val PREMIUM_OVERRIDES = listOf(
    OverrideAttr("player-license", "on-demand"),
    OverrideAttr("shuffle", false),
    OverrideAttr("on-demand", true),
    OverrideAttr("streaming", true),
    OverrideAttr("pick-and-shuffle", false),
    OverrideAttr("streaming-rules", ""),
    OverrideAttr("nft-disabled", "1"),
    OverrideAttr("can_use_superbird", true, false),
    OverrideAttr("tablet-free", false, false)
)

private val REMOVED_HOME_SECTIONS = setOf(20, 21)
private val REMOVED_BROWSE_SECTIONS = setOf(6)

private fun shallowClone(original: Any): Any {
    val unsafeClass = Class.forName("sun.misc.Unsafe")
    val theUnsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
    val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
    val clone = allocateInstance.invoke(theUnsafe, original.javaClass)
    var curr: Class<*>? = original.javaClass
    while (curr != null && curr != Any::class.java) {
        for (f in curr.declaredFields) {
            if (Modifier.isStatic(f.modifiers)) continue
            f.isAccessible = true
            f.set(clone, f.get(original))
        }
        curr = curr.superclass
    }
    return clone
}

private fun createOverriddenAttributesMap(originalMap: Map<String, *>): Map<String, *> {
    return try {
        val result = LinkedHashMap<String, Any?>(originalMap)
        for (override in PREMIUM_OVERRIDES) {
            val attr = result[override.key] ?: continue
            val originalVal = runCatching { XposedHelpers.getObjectField(attr, "value_") }.getOrNull()
            if (override.value == originalVal) continue
            val cloned = shallowClone(attr)
            XposedHelpers.setObjectField(cloned, "value_", override.value)
            result[override.key] = cloned
        }
        result
    } catch (_: Throwable) {
        originalMap
    }
}

val UnlockPremium = patch(
    name = "Unlock premium",
    description = "Enables on-demand playback, removes forced shuffle restrictions, and unlocks premium client attributes non-destructively.",
    use = true,
) {
    // 1: ProductStateProto attributes map override
    runCatching {
        val protoClass = classLoader.loadClass("com.spotify.remoteconfig.internal.ProductStateProto")
        val getMapMethod = protoClass.declaredMethods.firstOrNull {
            Map::class.java.isAssignableFrom(it.returnType) && it.parameterCount == 0
        }
        if (getMapMethod != null) {
            XposedBridge.hookMethod(getMapMethod, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val originalMap = param.result as? Map<String, *> ?: return
                    param.result = createOverriddenAttributesMap(originalMap)
                }
            })
        }
    }

    // 2: Disable forced shuffle in PlayerOptionOverrides
    runCatching {
        XposedHelpers.findAndHookMethod(
            "com.spotify.player.model.command.options.AutoValue_PlayerOptionOverrides\$Builder",
            classLoader,
            "build",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    runCatching {
                        XposedHelpers.callMethod(param.thisObject, "shufflingContext", false)
                    }
                }
            }
        )
    }

    // 3: Filter ad sections from Home
    runCatching {
        val homeClass = classLoader.loadClass("com.spotify.home.evopage.homeapi.proto.HomeStructure")
        val getSections = homeClass.declaredMethods.firstOrNull {
            it.name.contains("Sections") && List::class.java.isAssignableFrom(it.returnType)
        }
        if (getSections != null) {
            XposedBridge.hookMethod(getSections, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val list = param.result as? List<*> ?: return
                    param.result = list.filter { item ->
                        if (item == null) true
                        else {
                            val type = runCatching { XposedHelpers.getIntField(item, "featureTypeCase_") }.getOrDefault(0)
                            type !in REMOVED_HOME_SECTIONS
                        }
                    }
                }
            })
        }
    }

    // 4: Filter ad sections from Browse
    runCatching {
        val browseClass = classLoader.loadClass("com.spotify.browsita.v1.resolved.BrowseStructure")
        val getSections = browseClass.declaredMethods.firstOrNull {
            it.name.contains("Sections") && List::class.java.isAssignableFrom(it.returnType)
        }
        if (getSections != null) {
            XposedBridge.hookMethod(getSections, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val list = param.result as? List<*> ?: return
                    param.result = list.filter { item ->
                        if (item == null) true
                        else {
                            val type = runCatching { XposedHelpers.getIntField(item, "sectionTypeCase_") }.getOrDefault(0)
                            type !in REMOVED_BROWSE_SECTIONS
                        }
                    }
                }
            })
        }
    }
}
