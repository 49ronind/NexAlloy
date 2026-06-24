package io.github.nexalloy.morphe.x.downloads

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import app.morphe.extension.shared.Utils
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * When the download button is tapped, copies the direct media URL to clipboard
 * instead of (or in addition to) downloading. User-toggleable.
 *
 * Pref key: "x_media_link_handle"
 *   1 → download  |  2 → copy link  |  3 → show dialog  (mirrors piko Pref.vidMediaHandle)
 */
val CopyMediaLink = patch(
    name = "Copy media link",
    description = "Copies the direct media URL to clipboard when the download button is tapped.",
    use = false,
) {
    dependsOn(XVersionCheck)

    DownloadCallFingerprint.hookMethod {
        before { param ->
            try {
                val context = Utils.getContext() ?: return@before
                val mediaObj = param.args.firstOrNull() ?: return@before
                val urlField = mediaObj.javaClass.declaredFields
                    .firstOrNull { it.type == String::class.java }
                    ?.also { it.isAccessible = true } ?: return@before
                val url = urlField.get(mediaObj) as? String ?: return@before

                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("media_url", url))

                // Skip original download by returning early
                param.result = Unit
            } catch (_: Exception) {}
        }
    }
}
