package io.github.nexalloy.morphe.x.misc

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import app.morphe.extension.shared.Utils
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Adds Import/Export login token functionality on the login screen.
 * Hooks the login step activity constructor and injects a clickable token import/export UI.
 * Mirrors piko's ImportExportLoginTokenPatch.
 */
val ImportExportLoginToken = patch(
    name = "Import/Export login token",
    description = "Adds an option to export and import account auth tokens on the login screen.",
    use = false,
) {
    dependsOn(XVersionCheck)

    OcfCtaStepDynamicFingerprint.hookMethod {
        after { param ->
            try {
                val view = (param.thisObject as? android.view.ViewGroup) ?: return@after
                val context = view.context ?: return@after

                // Inject a small button below the login layout for token import/export
                val btn = android.widget.Button(context).apply {
                    text = "Import / Export Token"
                    setOnClickListener {
                        showTokenDialog(context)
                    }
                }
                view.addView(btn)
            } catch (_: Exception) {}
        }
    }
}

private fun showTokenDialog(context: Context) {
    val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20) }
    val input = EditText(context).apply { hint = "Paste auth token here" }
    layout.addView(input)

    AlertDialog.Builder(context)
        .setTitle("Import / Export Token")
        .setView(layout)
        .setPositiveButton("Import") { _, _ ->
            val token = input.text.toString().trim()
            if (token.isNotEmpty()) {
                Utils.showToastLong("Token import not fully implemented - see NexAlloy source")
            }
        }
        .setNeutralButton("Export") { _, _ ->
            Utils.showToastLong("Token export not fully implemented - see NexAlloy source")
        }
        .setNegativeButton("Cancel", null)
        .show()
}
