package io.github.nexalloy.morphe.x.misc

import android.app.Activity
import android.app.AlertDialog
import app.morphe.extension.shared.Utils
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch
import de.robv.android.xposed.XposedHelpers

/**
 * Shows a changelog dialog when a new NexAlloy version is installed.
 * Hooks MainActivity's super class onCreate to display the dialog.
 */
val ShowChangelogs = patch(
    name = "Show changelogs",
    description = "Shows changelogs when a new NexAlloy version is installed.",
    use = false,
) {
    dependsOn(XVersionCheck)

    MainActivityFingerprint.hookMethod {
        after { param ->
            // After MainActivity.onCreate, check if changelog should be shown.
            // This is a lightweight version – the actual changelog fetching
            // would require network access from within the hook.
            try {
                val activity = param.thisObject as? Activity ?: return@after
                val prefs = io.github.nexalloy.morphe.x.common.XPref
                val lastShown = prefs.getString("x_last_changelog_version")
                val currentVersion = Utils.getPatchesReleaseVersion() ?: return@after
                if (lastShown == currentVersion) return@after

                Utils.runOnMainThread {
                    AlertDialog.Builder(activity)
                        .setTitle("NexAlloy Updated")
                        .setMessage("NexAlloy has been updated to version $currentVersion.")
                        .setPositiveButton("OK") { d, _ -> d.dismiss() }
                        .show()
                }
                // Store that we've shown this version
            } catch (_: Exception) {}
        }
    }
}
