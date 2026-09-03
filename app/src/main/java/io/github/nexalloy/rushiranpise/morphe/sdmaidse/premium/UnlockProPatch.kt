package io.github.nexalloy.rushiranpise.morphe.sdmaidse.premium

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.patch

val UnlockPro = patch(
    name = "Unlock Pro",
    description = "Unlocks SD Maid SE Pro features.",
) {
    // Layer 1: Force isPro = true on any UpgradeRepoGplay$Info construction
    runCatching {
        val infoClass = UpgradeInfoClassFingerprint.declaredClass
        infoClass.declaredConstructors.forEach { constructor ->
            XposedBridge.hookMethod(constructor, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    runCatching {
                        XposedHelpers.setBooleanField(param.thisObject, "isPro", true)
                    }
                }
            })
        }
    }

    // Layer 2: Hook UpgradeRepoGplay$Info.isPro() getter to return true
    runCatching {
        UpgradeInfoClassFingerprint.hookMethod {
            before { param ->
                param.result = true
            }
        }
    }

    // Layer 3: Short-circuit UpgradeRepoExtensionsKt.isPro(UpgradeRepoGplay, ContinuationImpl) coroutine
    runCatching {
        IsProSuspendFingerprint.hookMethod {
            before { param ->
                param.result = java.lang.Boolean.TRUE
            }
        }
    }
}
