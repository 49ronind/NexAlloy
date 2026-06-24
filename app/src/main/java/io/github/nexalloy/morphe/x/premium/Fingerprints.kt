package io.github.nexalloy.morphe.x.premium

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

/** PiP eligibility check (returns boolean). */
internal object ForcePipFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf(
        "android_immersive_media_player_native_pip_enabled",
        "isPipAvailable",
    ),
)

/** Undo-posts timer check (returns long). */
internal object UndoPostsFingerprint : Fingerprint(
    returnType = "J",
    strings = listOf("undo_tweet_delay_ms", "undo_tweet_enabled"),
)

/** Premium subscription check used for download gating. */
internal object PremiumSubscriptionCheckFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf(
        "subscribed",
        "is_premium_subscriber",
        "media_hd_download_enabled",
    ),
)

/** Bookmark-tab redirect check. */
internal object BookmarkTabRedirectFingerprint : Fingerprint(
    strings = listOf("bookmarks", "tab_redirect_bookmark"),
)

/** "Create List" gate that checks premium status. */
internal object CreateListGateFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("is_list_creation_enabled", "list_creation_requires_premium"),
)
