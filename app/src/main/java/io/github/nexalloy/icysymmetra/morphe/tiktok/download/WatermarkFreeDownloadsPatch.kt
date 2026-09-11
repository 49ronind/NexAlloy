package io.github.nexalloy.icysymmetra.morphe.tiktok.download

import io.github.nexalloy.callMethodOrNull
import io.github.nexalloy.getObjectFieldOrNull
import io.github.nexalloy.patch
import io.github.nexalloy.setObjectField

private fun hasUsableUrl(model: Any?): Boolean {
    if (model == null) return false
    val urls = model.callMethodOrNull("getUrlList") as? List<*>
        ?: model.getObjectFieldOrNull("urlList") as? List<*>
        ?: return false
    return urls.any { url ->
        (url as? String)?.isNotBlank() == true && !url.equals("null", ignoreCase = true)
    }
}

private fun selectCleanFallback(video: Any): Any? {
    val h264 = video.getObjectFieldOrNull("h264PlayAddr")
        ?: video.callMethodOrNull("getH264PlayAddr")
    if (hasUsableUrl(h264)) return h264

    val playAddr = video.getObjectFieldOrNull("playAddr")
        ?: video.callMethodOrNull("getPlayAddr")
    if (hasUsableUrl(playAddr)) return playAddr

    return null
}

fun patchVideoObject(video: Any?) {
    if (video == null) return
    runCatching {
        val original = video.getObjectFieldOrNull("downloadNoWatermarkAddr")
            ?: video.callMethodOrNull("getDownloadNoWatermarkAddr")
        if (hasUsableUrl(original)) return

        val fallback = selectCleanFallback(video) ?: return
        runCatching { video.setObjectField("downloadNoWatermarkAddr", fallback) }
        runCatching { video.callMethodOrNull("setDownloadNoWatermarkAddr", fallback) }
        runCatching { video.setObjectField("downloadAddr", fallback) }
        runCatching { video.callMethodOrNull("setDownloadAddr", fallback) }
    }
}

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
    runCatching {
        AwemeGetVideoFingerprint.hookMethod {
            after { param ->
                val video = param.result ?: return@after
                patchVideoObject(video)
            }
        }
    }
}

