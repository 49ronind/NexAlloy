package io.github.nexalloy.icysymmetra.morphe.tiktok.ad

import io.github.nexalloy.callMethodOrNull
import io.github.nexalloy.getObjectFieldOrNull
import io.github.nexalloy.patch

val FeedFilter = patch(
    name = "Feed ad removal",
    description = "Suppresses sponsored feed items, ads, and floating promotional badges.",
) {
    runCatching {
        MainFeedResponseFingerprint.hookMethod {
            after { param ->
                val feedList = param.result ?: return@after
                val items = (feedList.getObjectFieldOrNull("items")
                    ?: feedList.callMethodOrNull("getItems")) as? MutableList<*> ?: return@after
                items.removeAll { aweme ->
                    if (aweme == null) return@removeAll false
                    val isAd = aweme.callMethodOrNull("isAd") as? Boolean ?: false
                    val isSoftAd = aweme.callMethodOrNull("isSoftAd") as? Boolean ?: false
                    isAd || isSoftAd
                }
            }
        }
    }

    runCatching {
        AwemeIsAdFingerprint.hookMethod {
            before { param ->
                param.result = false
            }
        }
    }

    runCatching {
        TouchPointPendantParserFingerprint.hookMethod {
            before { param ->
                val pendantList = param.args.getOrNull(1) as? MutableList<*>
                pendantList?.clear()
                param.result = null
            }
        }
    }
}
