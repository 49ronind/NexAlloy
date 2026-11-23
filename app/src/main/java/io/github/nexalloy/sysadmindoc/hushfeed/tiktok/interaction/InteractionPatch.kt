package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.interaction

import android.app.Activity
import android.os.Build
import android.view.Window
import android.view.WindowManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch

val DisableScreenCaptureDetection = patch(
    name = "Disable screen capture detection",
    description = "Stops TikTok from detecting screenshots and screen recordings (HushFeed).",
) {
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
}

val DisableLongPressRepost = patch(
    name = "Disable long-press repost",
    description = "Consumes long-press on the Like button so it neither reposts nor triggers a like (HushFeed).",
) {
    runCatching {
        LongPressRepostGateFingerprint.hookMethod {
            before { param ->
                // Return true to consume the long-click without falling through to a tap/like.
                param.result = true
            }
        }
    }
}

val DisableLongPressQuickShare = patch(
    name = "Disable long-press quick share",
    description = "Prevents holding the Share button from opening TikTok's quick-share menu (HushFeed).",
    use = false,
) {
    runCatching {
        LongPressQuickShareGateCallerFingerprint.hookMethod {
            before { param ->
                param.result = false
            }
        }
    }
}

val AllowScreenCapture = patch(
    name = "Allow screenshots and Circle to Search",
    description = "Clears FLAG_SECURE on TikTok windows and lifts Circle to Search restrictions (HushFeed).",
    use = false,
) {
    runCatching {
        val secureFlag = WindowManager.LayoutParams.FLAG_SECURE
        XposedBridge.hookAllMethods(
            Window::class.java,
            "addFlags",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val flags = param.args.getOrNull(0) as? Int ?: return
                    if ((flags and secureFlag) != 0) {
                        param.args[0] = flags and secureFlag.inv()
                    }
                }
            },
        )
        XposedBridge.hookAllMethods(
            Window::class.java,
            "setFlags",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val flags = param.args.getOrNull(0) as? Int ?: return
                    if ((flags and secureFlag) != 0) {
                        param.args[0] = flags and secureFlag.inv()
                    }
                }
            },
        )
        XposedBridge.hookAllMethods(
            Window::class.java,
            "setAttributes",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val attrs = param.args.getOrNull(0) as? WindowManager.LayoutParams ?: return
                    if ((attrs.flags and secureFlag) != 0) {
                        attrs.flags = attrs.flags and secureFlag.inv()
                    }
                }
            },
        )
    }
    runCatching {
        CircleSearchBlockFingerprint.hookMethod {
            after { param ->
                if (param.result is Boolean) {
                    param.result = false
                }
            }
        }
    }
}
