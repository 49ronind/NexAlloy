package io.github.nexalloy.rushiranpise.morphe.sdmaidse.premium

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.patch

val UnlockPro = patch(
    name = "Unlock Pro",
    description = "Unlocks SD Maid SE Pro features.",
) {
    // Layer 1: Hook all UpgradeRepoGplay$Info constructors to force isPro = true, isSettled = true, error = null
    runCatching {
        val infoClass = classLoader.loadClass("eu.darken.sdmse.common.upgrade.core.UpgradeRepoGplay\$Info")
        infoClass.declaredConstructors.forEach { constructor ->
            XposedBridge.hookMethod(constructor, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    runCatching {
                        XposedHelpers.setBooleanField(param.thisObject, "isPro", true)
                        XposedHelpers.setBooleanField(param.thisObject, "isSettled", true)
                        XposedHelpers.setObjectField(param.thisObject, "error", null)
                    }
                }
            })
        }
    }

    // Layer 2: Hook UpgradeRepoGplay$Info.getHasAutoRenewingSubscription
    runCatching {
        val infoClass = classLoader.loadClass("eu.darken.sdmse.common.upgrade.core.UpgradeRepoGplay\$Info")
        XposedHelpers.findAndHookMethod(
            infoClass,
            "getHasAutoRenewingSubscription",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = true
                }
            }
        )
    }

    // Layer 3: Short-circuit UpgradeRepoExtensionsKt.isPro(UpgradeRepoGplay, ContinuationImpl) coroutine
    runCatching {
        IsProSuspendFingerprint.hookMethod {
            before { param ->
                param.result = java.lang.Boolean.TRUE
            }
        }
    }

    // Layer 4: Hook all isPro* extension methods in UpgradeRepoExtensionsKt
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
