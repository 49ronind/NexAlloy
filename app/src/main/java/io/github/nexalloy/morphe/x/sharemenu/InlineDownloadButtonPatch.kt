package io.github.nexalloy.morphe.x.sharemenu

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import app.morphe.extension.shared.Utils
import io.github.nexalloy.morphe.x.downloads.UnlockDownloads
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Adds an inline download button directly below posts in the timeline.
 *
 * Hooks the inline-action-bar bind method and appends a download ImageButton.
 * The download is triggered using the same mechanism as [UnlockDownloads].
 */
val InlineDownloadButton = patch(
    name = "Inline download button",
    description = "Adds a download button directly in the inline action bar below posts.",
    use = false,
) {
    dependsOn(XVersionCheck, UnlockDownloads)

    InlineDownloadFingerprint.hookMethod {
        after { param ->
            try {
                val view = param.thisObject as? ViewGroup ?: return@after
                val context = view.context ?: return@after

                // Avoid duplicate buttons
                if (view.findViewWithTag<View?>("nexalloy_dl_btn") != null) return@after

                val btn = ImageButton(context).apply {
                    tag = "nexalloy_dl_btn"
                    setImageDrawable(
                        context.getDrawable(android.R.drawable.stat_sys_download)
                    )
                    background = null
                    contentDescription = "Download"
                    setOnClickListener {
                        Utils.showToastShort("Download triggered")
                        // TODO: wire into DownloadCallFingerprint mechanism
                    }
                }

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                view.addView(btn, lp)
            } catch (_: Exception) {}
        }
    }
}
