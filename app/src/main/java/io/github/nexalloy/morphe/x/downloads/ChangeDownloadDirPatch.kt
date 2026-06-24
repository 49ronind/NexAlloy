package io.github.nexalloy.morphe.x.downloads

import android.os.Environment
import io.github.nexalloy.morphe.x.common.XPref
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch
import java.io.File

/**
 * Redirects video downloads to a user-specified folder.
 * Preference keys (stored by NexAlloy settings):
 *   x_download_public_folder  – base public folder (default: Downloads)
 *   x_download_subfolder      – sub-folder name    (default: X)
 */
val ChangeDownloadDir = patch(
    name = "Custom download folder",
    description = "Changes the download directory for video downloads.",
    use = false,
) {
    dependsOn(XVersionCheck)

    DownloadPathFingerprint.hookMethod {
        before { param ->
            val base = XPref.getString("x_download_public_folder")
                .ifEmpty { Environment.DIRECTORY_DOWNLOADS }
            val sub = XPref.getString("x_download_subfolder").ifEmpty { "X" }
            val folder = File(
                Environment.getExternalStoragePublicDirectory(base), sub
            ).also { it.mkdirs() }

            // Replace the destination directory argument (first String arg = filename/url)
            // The piko approach hooks DownloadManager.Request.setDestinationInExternalPublicDir
            // which takes (Environment dir, String subpath).
            // We intercept the DownloadPathFingerprint method and override args[1] (public folder).
            // arg[0] = parsed URL, arg[1] = guessed filename → we patch at the point the folder
            // is set, so we override the outer argument that carries the folder string.
            try {
                // arg index 1 is typically the public folder string in DownloadManager usage
                if (param.args.size > 1 && param.args[1] is String) {
                    param.args[1] = folder.absolutePath
                }
            } catch (_: Exception) {}
        }
    }
}
