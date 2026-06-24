package io.github.nexalloy.morphe.x.ads

import app.morphe.extension.shared.Logger
import io.github.nexalloy.getObjectFieldOrNull
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

// ────────────────────────────────────────────────────────────────────────────
// Runtime entry-id filter (mirrors piko's TimelineEntry.isEntryIdRemove)
// ────────────────────────────────────────────────────────────────────────────

internal fun shouldRemoveEntry(entryId: String?): Boolean {
    if (entryId == null) return false
    val parts = entryId.split("-")
    val prefix = parts[0]
    if (prefix == "cursor" || prefix == "Guide" || prefix.startsWith("semantic_core")) return false

    if (entryId.contains("promoted"))  return true
    if (prefix == "conversationthread" && parts.size == 3) return true
    if (prefix == "superhero" || prefix == "eventsummary") return true
    if (entryId.contains("rtb"))       return true
    if (prefix == "main-event-" || prefix == "pivot") return true
    return false
}

internal fun shouldRemoveRecommendation(entryId: String?): Boolean {
    if (entryId == null) return false
    return entryId.startsWith("who-to-follow")
        || entryId.startsWith("who-to-subscribe")
        || entryId.startsWith("community-to-join")
        || entryId.startsWith("tweetdetailrelatedtweets")
        || entryId.startsWith("bookmarked")
        || entryId.startsWith("pinned-tweets")
        || entryId.startsWith("messageprompt-")
        || entryId.startsWith("toptabsrpusermodule")
        || entryId.startsWith("stories")
}

// ────────────────────────────────────────────────────────────────────────────
// Patches
// ────────────────────────────────────────────────────────────────────────────

/** Hooks JsonTimelineEntry.parse() and filters out ad/promoted entries. */
val HideAds = patch(
    name = "Remove Ads",
    description = "Removes promoted posts, promoted trends and Google ads from timelines.",
) {
    dependsOn(XVersionCheck)

    // Hook main timeline entry parser
    TimelineEntryParseFingerprint.hookMethod {
        after { param ->
            val entry = param.result ?: return@after
            val entryId = entry.getObjectFieldOrNull("a") as? String
            if (shouldRemoveEntry(entryId)) {
                Logger.printDebug { "HideAds: removed entry $entryId" }
                param.result = null
            }
        }
    }

    // Hook module-item parser (inline ad units inside threads)
    TimelineModuleItemParseFingerprint.hookMethod {
        after { param ->
            val item = param.result ?: return@after
            val entryId = item.getObjectFieldOrNull("a") as? String
            if (shouldRemoveEntry(entryId)) {
                Logger.printDebug { "HideAds: removed module item $entryId" }
                param.result = null
            }
        }
    }

    // Hook promoted trends
    HidePromotedTrendFingerprint.hookMethod {
        after { param ->
            // The last object built before return is the trend data; null it to hide
            val result = param.result
            if (result != null) {
                Logger.printDebug { "HideAds: nullified promoted trend" }
                param.result = null
            }
        }
    }
}

/** Hides recommendation items (Who to Follow, Today's News, etc.). */
val HideRecommendationItems = patch(
    name = "Hide recommendation items",
    description = "Hides Who to Follow, Today's news, Communities to join, and other recommendation items.",
) {
    dependsOn(XVersionCheck)

    TimelineEntryParseFingerprint.hookMethod {
        after { param ->
            val entry = param.result ?: return@after
            val entryId = entry.getObjectFieldOrNull("a") as? String
            if (shouldRemoveRecommendation(entryId)) {
                Logger.printDebug { "HideRecommendations: removed $entryId" }
                param.result = null
            }
        }
    }

    TimelineModuleItemParseFingerprint.hookMethod {
        after { param ->
            val item = param.result ?: return@after
            val entryId = item.getObjectFieldOrNull("a") as? String
            if (shouldRemoveRecommendation(entryId)) {
                param.result = null
            }
        }
    }
}
