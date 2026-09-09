package io.github.nexalloy.icysymmetra.morphe.tiktok.interaction

import android.app.Activity
import android.os.Build
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch

val InteractionEnhancements = patch(
    name = "Interaction enhancements",
    description = "Suppresses screen recording detection, stops automatic video looping, disables long-press repost, and restores seekbar.",
) {
    // Screen recording detection suppression
    runCatching {
        AntiRecordingAddedFingerprint.hookMethod {
            before { param ->
                param.result = null
            }
        }
    }
    runCatching {
        AntiRecordingRemovedFingerprint.hookMethod {
            before { param ->
                param.result = null
            }
        }
    }
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val stubHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            }
            XposedBridge.hookAllMethods(Activity::class.java, "registerScreenCaptureCallback", stubHook)
            XposedBridge.hookAllMethods(Activity::class.java, "unregisterScreenCaptureCallback", stubHook)
        }
    }

    // Stop video looping
    runCatching {
        VideoEngineSetLoopingFingerprint.hookMethod {
            before { param ->
                param.args[0] = false
            }
        }
    }

    // Disable long press repost
    runCatching {
        LongPressRepostGateFingerprint.hookMethod {
            before { param ->
                param.result = false
            }
        }
    }

    // Show seekbar
    runCatching {
        SetSeekBarShowTypeFingerprint.hookMethod {
            before { param ->
                val type = param.args.firstOrNull() as? Int ?: return@before
                if (type == 3 || type == 4) {
                    param.args[0] = 0
                }
            }
        }
    }
}
