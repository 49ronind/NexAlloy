package io.github.nexalloy.morphe.x.link

import android.app.Activity
import android.content.Intent
import android.net.Uri
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Default hosts that are re-written to twitter.com deep links inside UrlInterpreterActivity. */
private val DEFAULT_CUSTOM_HOSTS = setOf(
    "vxtwitter.com", "fixvx.com", "fxtwitter.com", "fixupx.com",
    "twittpr.com", "xcancel.com",
)

/**
 * Intercepts UrlInterpreterActivity.onCreate and rewrites custom sharing
 * domains (vxtwitter, fxtwitter, etc.) to the equivalent x.com URL so X can open them natively.
 * Mirrors piko's HandleCustomDeepLinksPatch.
 */
val HandleCustomDeepLinks = patch(
    name = "Handle custom twitter links",
    description = "Opens vxtwitter / fxtwitter / fixupx links directly inside X.",
) {
    dependsOn(XVersionCheck)

    UrlInterpreterActivityCreateFingerprint.hookMethod {
        before { param ->
            try {
                val activity = param.thisObject as? Activity ?: return@before
                val uri = activity.intent?.data ?: return@before
                val host = uri.host?.lowercase() ?: return@before

                val isCustomHost = DEFAULT_CUSTOM_HOSTS.any { host == it || host.endsWith(".$it") }
                if (!isCustomHost) return@before

                // Rewrite host to x.com, keeping path and query
                val newUri = uri.buildUpon().authority("x.com").build()
                val newIntent = Intent(Intent.ACTION_VIEW, newUri).apply {
                    setPackage(activity.packageName)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                activity.startActivity(newIntent)
                activity.finish()
                param.result = Unit // skip original onCreate
            } catch (_: Exception) {}
        }
    }
}
