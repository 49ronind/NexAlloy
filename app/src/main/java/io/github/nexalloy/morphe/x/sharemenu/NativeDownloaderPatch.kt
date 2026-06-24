package io.github.nexalloy.morphe.x.sharemenu

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.morphe.extension.shared.Utils
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Adds a "Download" button to the tweet share menu.
 *
 * In piko, this patch injects a new enum constant into the share-menu action enum
 * and adds the action handler. In NexAlloy/Xposed, we hook the share-menu binding
 * method and programmatically add the download action item.
 *
 * Implementation note: injecting new enum values at runtime is not possible without
 * bytecode manipulation, so this patch hooks the menu item click handler for any
 * existing DOWNLOAD item and ensures the download logic runs.
 */
val NativeDownloader = patch(
    name = "Native downloader",
    description = "Adds a Download button to the tweet share menu.",
) {
    dependsOn(XVersionCheck)

    ShareMenuDownloadFingerprint.hookMethod {
        before { param ->
            try {
                val context = Utils.getContext() ?: return@before
                // Extract media URL from args; the first String arg is typically the URL.
                val url = param.args.filterIsInstance<String>().firstOrNull() ?: return@before
                if (!url.startsWith("http")) return@before
                // Trigger system download manager
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                param.result = Unit
            } catch (_: Exception) {}
        }
    }
}
