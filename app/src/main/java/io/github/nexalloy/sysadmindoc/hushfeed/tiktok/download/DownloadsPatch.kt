package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.download

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.callMethodOrNull
import io.github.nexalloy.getObjectFieldOrNull
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import io.github.nexalloy.setObjectField

private val H264_PLAY_ADDR_FIELDS = arrayOf("h264PlayAddrValue", "h264PlayAddr")
private val PLAY_ADDR_FIELDS = arrayOf("playAddrValue", "playAddr")

private fun hasUsableUrl(model: Any?): Boolean {
    if (model == null) return false
    val urls = model.callMethodOrNull("getUrlList") as? List<*>
        ?: model.getObjectFieldOrNull("urlList") as? List<*>
        ?: return false
    return urls.any { url ->
        (url as? String)?.trim()?.let { it.isNotEmpty() && !it.equals("null", ignoreCase = true) } == true
    }
}

/**
 * Reads play addresses directly from fields first (matching HushFeed 46.9.3–47.9.3):
 * 46.9.3+ renamed both fields to `...Value`, and calling getters mutates codec/ratio/expiry.
 */
private fun readFieldFirst(target: Any, names: Array<String>): Any? {
    for (name in names) {
        val value = target.getObjectFieldOrNull(name)
        if (value != null) return value
    }
    return null
}

private fun selectCleanFallback(video: Any): Any? {
    val h264 = readFieldFirst(video, H264_PLAY_ADDR_FIELDS)
        ?: video.callMethodOrNull("getH264PlayAddr")
    if (hasUsableUrl(h264)) return h264

    val playAddr = readFieldFirst(video, PLAY_ADDR_FIELDS)
        ?: video.callMethodOrNull("getPlayAddr")
    if (hasUsableUrl(playAddr)) return playAddr

    return null
}

internal fun patchVideoObject(video: Any?) {
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
    description = "Enables downloads even when restricted, removes video and comment-photo watermarks, and skips the post-download share sheet (HushFeed).",
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
                patchVideoObject(param.result)
            }
        }
    }
    // Strip drawn [tiktok_logo] watermark when saving comment photos.
    runCatching {
        val drawBitmap = XposedHelpers.findMethodExact(
            Canvas::class.java,
            "drawBitmap",
            Bitmap::class.java,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Paint::class.java,
        )
        CommentImageWatermarkFingerprint.hookMethod(
            scopedHook(drawBitmap) {
                before { param ->
                    param.result = null
                }
            },
        )
    }
    // Skip TikTok's post-download "Share to" bottom sheet.
    runCatching {
        PostDownloadShareSheetFingerprint.hookMethod {
            before { param ->
                param.result = null
            }
        }
    }
}
