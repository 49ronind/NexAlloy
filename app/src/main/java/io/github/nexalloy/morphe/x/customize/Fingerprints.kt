package io.github.nexalloy.morphe.x.customize

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

// ── Navigation bar ───────────────────────────────────────────────────────────

/** Main nav-bar tab list builder. */
internal object NavBarBuilderFingerprint : Fingerprint(
    strings = listOf(
        "home", "explore", "notifications",
        "messages", "premium", "communities",
    ),
)

/** Second nav-bar variant (post 11.88). */
internal object NavBarBuilder2Fingerprint : Fingerprint(
    strings = listOf(
        "HOME_TIMELINE", "SEARCH",
        "NOTIFICATIONS", "DIRECT_MESSAGES",
    ),
)

// ── Side bar ─────────────────────────────────────────────────────────────────

/** Side-drawer item list. */
internal object SideBarBuilderFingerprint : Fingerprint(
    strings = listOf("Profile", "Twitter Blue", "Topics", "Bookmarks", "Lists"),
)

// ── Timeline top bar ─────────────────────────────────────────────────────────

/** Home-timeline tab list (Following, For You, etc.). */
internal object TimelineTabsBuilderFingerprint : Fingerprint(
    strings = listOf("timeline_tab_following", "timeline_tab_for_you"),
)

// ── Profile tabs ─────────────────────────────────────────────────────────────

/** Profile-page tab list (Tweets, Replies, Media, Likes). */
internal object ProfileTabsBuilderFingerprint : Fingerprint(
    strings = listOf(
        "profile_tab_tweets", "profile_tab_replies",
        "profile_tab_media", "profile_tab_likes",
    ),
)

// ── Explore / Search tabs ────────────────────────────────────────────────────

/** Explore section tab list. */
internal object ExploreTabsBuilderFingerprint : Fingerprint(
    strings = listOf(
        "trending", "news", "sports", "entertainment",
    ),
)

/** Search results tab list. */
internal object SearchTabsBuilderFingerprint : Fingerprint(
    strings = listOf(
        "search_tab_top", "search_tab_latest",
        "search_tab_people", "search_tab_media",
    ),
)

// ── Notification tabs ────────────────────────────────────────────────────────

/** Notification-screen tab list. */
internal object NotificationTabsBuilderFingerprint : Fingerprint(
    strings = listOf(
        "notification_tab_all", "notification_tab_verified",
        "notification_tab_mentions",
    ),
)

// ── Inline action bar ────────────────────────────────────────────────────────

/** Inline (under-tweet) action bar item list. */
internal object InlineBarBuilderFingerprint : Fingerprint(
    strings = listOf(
        "reply", "retweet", "like", "share",
    ),
    custom = { _, cls -> cls.contains("inlineactions") },
)

// ── Type-ahead (search-as-you-type) ─────────────────────────────────────────

/** Type-ahead suggestion-response filter. */
internal object TypeAheadResponseFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/search/typeahead/",
    returnType = "Ljava/util/List;",
    strings = listOf("type", "query"),
)

// ── Reply sorting ────────────────────────────────────────────────────────────

/** Default reply-sort option stored in prefs. */
internal object DefaultReplySortingFingerprint : Fingerprint(
    strings = listOf(
        "ranking_mode",
        "RELEVANCE", "RECENCY",
    ),
)

// ── Post font size ───────────────────────────────────────────────────────────

/** TextSizePreference / density-scaled text size setter for post body. */
internal object PostFontSizeFingerprint : Fingerprint(
    definingClass = "/textsize/",
    returnType = "F",
    accessFlags = listOf(AccessFlags.PUBLIC),
)

// ── Font hook ────────────────────────────────────────────────────────────────

/** emoji2 EmojiCompat CharSequence – used to inject custom emoji font. */
internal object CustomFontHookFingerprint : Fingerprint(
    definingClass = "emoji2/text",
    filters = listOf(string("end should be < than charSequence length")),
)

/** Typeface.createFromAsset hook for custom post font. */
internal object TypefaceCreateFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/ui/components/text/",
    strings = listOf("fonts/chirp-regular-web.woff2", "Chirp-Regular"),
)
