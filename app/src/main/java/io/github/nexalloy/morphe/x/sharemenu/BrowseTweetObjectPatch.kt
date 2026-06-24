package io.github.nexalloy.morphe.x.sharemenu

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.morphe.extension.shared.Utils
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Adds a "Browse tweet object" option to the share menu that opens
 * the raw tweet JSON API URL in the browser.
 * Mirrors piko's BrowseTweetObjectPatch.
 */
val BrowseTweetObject = patch(
    name = "Browse tweet object",
    description = "Opens the raw tweet API JSON in a browser from the share menu.",
    use = false,
) {
    dependsOn(XVersionCheck)

    BrowseObjectFingerprint.hookMethod {
        before { param ->
            try {
                val context = Utils.getContext() ?: return@before
                // First String arg is the tweet ID
                val tweetId = param.args.filterIsInstance<String>().firstOrNull() ?: return@before
                val url = "https://api.twitter.com/2/tweets/$tweetId" +
                    "?expansions=author_id&tweet.fields=created_at,text"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                param.result = Unit
            } catch (_: Exception) {}
        }
    }
}
