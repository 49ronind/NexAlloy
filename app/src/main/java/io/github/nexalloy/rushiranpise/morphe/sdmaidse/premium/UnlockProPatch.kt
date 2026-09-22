package io.github.nexalloy.rushiranpise.morphe.sdmaidse.premium

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.patch

val UnlockPro = patch(
    name = "Unlock Pro",
    description = "Unlocks SD Maid SE Pro features.",
) {
    val targetInfoClasses = listOf(
        "eu.darken.sdmse.common.upgrade.core.UpgradeRepoGplay\$Info",
        "eu.darken.sdmse.common.upgrade.core.UpgradeRepoFoss\$Info",
    )

    for (className in targetInfoClasses) {
        runCatching {
            val infoClass = classLoader.loadClass(className)

            // Layer 1: Hook all constructors — dynamic field overwrite
            // Sets all boolean fields (isPro, isSettled, gracePeriod) to true regardless of R8 obfuscation
            infoClass.declaredConstructors.forEach { constructor ->
                XposedBridge.hookMethod(constructor, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val instance = param.thisObject ?: return
                        var currentClass: Class<*>? = infoClass
                        while (currentClass != null && currentClass != Any::class.java) {
                            currentClass.declaredFields.forEach { field ->
                                runCatching {
                                    field.isAccessible = true
                                    if (field.type == java.lang.Boolean.TYPE) {
                                        field.setBoolean(instance, true)
                                    } else if (field.type == java.lang.Boolean::class.java) {
                                        field.set(instance, java.lang.Boolean.TRUE)
                                    } else if (Throwable::class.java.isAssignableFrom(field.type)) {
                                        field.set(instance, null)
                                    }
                                }
                            }
                            currentClass = currentClass.superclass
                        }
                    }
                })
            }

            // Layer 2: Hook all 0-argument boolean getters on Info
            // Catches isPro(), isSettled(), hasAutoRenewingSubscription, component1(), component4(), etc.
            infoClass.declaredMethods.filter {
                it.parameterTypes.isEmpty() && (it.returnType == java.lang.Boolean.TYPE || it.returnType == java.lang.Boolean::class.java)
            }.forEach { method ->
                runCatching {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = if (method.returnType == java.lang.Boolean.TYPE) true else java.lang.Boolean.TRUE
                        }
                    })
                }
            }

            // Layer 3: Hook any 0-argument Throwable getters to return null (clear errors)
            infoClass.declaredMethods.filter {
                it.parameterTypes.isEmpty() && Throwable::class.java.isAssignableFrom(it.returnType)
            }.forEach { method ->
                runCatching {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = null
                        }
                    })
                }
            }
        }
    }

    // Layer 4: Legacy / FOSS short-circuit UpgradeRepoExtensionsKt.isPro coroutine if present
    runCatching {
        classLoader.loadClass("eu.darken.sdmse.common.upgrade.UpgradeRepoExtensionsKt")
        IsProSuspendFingerprint.hookMethod {
            before { param ->
                param.result = java.lang.Boolean.TRUE
            }
        }
    }

    // Layer 5: Legacy / FOSS hook all isPro* extension methods if present
    runCatching {
        val extClass = classLoader.loadClass("eu.darken.sdmse.common.upgrade.UpgradeRepoExtensionsKt")
        extClass.declaredMethods.filter { it.name.startsWith("isPro") }.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = java.lang.Boolean.TRUE
                }
            })
        }
    }
}
