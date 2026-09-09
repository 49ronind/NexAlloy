package io.github.nexalloy.icysymmetra.morphe.tiktok.download

import io.github.nexalloy.patch

val WatermarkFreeDownloads = patch(
    name = "Watermark-free downloads",
    description = "Enables the native download option and removes watermarks on downloaded media.",
) {
    runCatching {
        AclCommonShareGetCodeFingerprint.hookMethod {
            before { param ->
                param.result = 0
            }
        }
    }
    runCatching {
        AclCommonShareGetShowTypeFingerprint.hookMethod {
            before { param ->
                param.result = 2
            }
        }
    }
    runCatching {
        AclCommonShareGetTranscodeFingerprint.hookMethod {
            before { param ->
                param.result = 1
            }
        }
    }
}
