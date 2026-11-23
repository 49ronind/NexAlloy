package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.playback

import io.github.nexalloy.patch

val ShowSeekbar = patch(
    name = "Show progress bar",
    description = "Always shows the video progress bar on feed videos where TikTok hides it (HushFeed).",
) {
    runCatching {
        ShouldShowProgressBarFingerprint.hookMethod {
            before { param ->
                if (param.args.firstOrNull() != null) {
                    param.result = true
                }
            }
        }
    }
    runCatching {
        ::setSeekBarShowTypeMethod.hookMethod {
            before { param ->
                val lastIdx = param.args.lastIndex
                if (lastIdx >= 0) {
                    val type = param.args[lastIdx] as? Int ?: return@before
                    if (type == 3 || type == 4) {
                        param.args[lastIdx] = 0
                    }
                }
            }
        }
    }
}

val StopVideoLooping = patch(
    name = "Stop video looping",
    description = "Stops videos at the end instead of looping automatically (HushFeed).",
    use = false,
) {
    runCatching {
        VideoEngineSetLoopingFingerprint.hookMethod {
            before { param ->
                param.args[0] = false
            }
        }
    }
}
