package io.github.nexalloy.morphe.x

import io.github.nexalloy.morphe.x.ads.HideAds
import io.github.nexalloy.morphe.x.ads.HideRecommendationItems
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.morphe.x.customize.*
import io.github.nexalloy.morphe.x.downloads.ChangeDownloadDir
import io.github.nexalloy.morphe.x.downloads.CopyMediaLink
import io.github.nexalloy.morphe.x.downloads.UnlockDownloads
import io.github.nexalloy.morphe.x.featureflag.*
import io.github.nexalloy.morphe.x.link.*
import io.github.nexalloy.morphe.x.misc.*
import io.github.nexalloy.morphe.x.premium.*
import io.github.nexalloy.morphe.x.sharemenu.*
import io.github.nexalloy.morphe.x.timeline.*

/**
 *
 * Ordering rules:
 *  1. [XVersionCheck] must be FIRST – it initialises [XPref] and version flags.
 *  2. Base/hook patches (FeatureFlagHook, TweetInfoHook) must precede their dependants.
 *  3. NavBarFix is internal and is declared before CustomNavBar in [featureflag] package.
 */
val XPatches = arrayOf(
    // ── Infrastructure (must be first) ───────────────────────────────────────
    XVersionCheck,

    // ── Ads ──────────────────────────────────────────────────────────────────
    HideAds,
    HideRecommendationItems,

    // ── Feature flags ────────────────────────────────────────────────────────
    FeatureFlagHook,          // base hook – registers XFeatureFlags interceptor
    DisableChirpFont,
    HideBookmarkInTimeline,
    HideFABMenuButtons,
    HideImmersivePlayer,
    RemoveViewCount,
    NavBarFix,                // internal – enables nav-bar customisation flags

    // ── Downloads ────────────────────────────────────────────────────────────
    UnlockDownloads,
    ChangeDownloadDir,
    CopyMediaLink,

    // ── Links / URLs ─────────────────────────────────────────────────────────
    ClearTrackingParams,
    HandleCustomDeepLinks,
    CustomSharingDomain,
    LegacyShareLinks,
    NoShortenedUrl,

    // ── Timeline – TweetInfo base ────────────────────────────────────────────
    TweetInfoHook,            // base hook – runs XTweetInfoProcessors
    HideCommunityNotes,
    HidePromoteButton,
    ForceTranslate,

    // ── Timeline – hide/show ─────────────────────────────────────────────────
    HideBanner,
    HideCommunityBadge,
    HideHiddenReplies,
    HideLiveThreads,
    HideNavBarBadges,
    HideNudgeButton,
    HidePostMetrics,
    HideSocialProof,
    RemovePremiumUpsell,
    ShowSensitiveMedia,
    ShowSourceLabel,
    ShowPollResults,

    // ── Timeline – behaviour ─────────────────────────────────────────────────
    DisableAutoScroll,
    EnableVidAutoAdvance,
    ForceHD,
    DeleteFromDatabase,

    // ── Misc ─────────────────────────────────────────────────────────────────
    BlockRedirectToXLite,
    DisUnifyXChatSystem,
    HideFAB,
    ImportExportLoginToken,
    HideRecommendedUsers,
    RoundOffNumbers,
    PauseSearchSuggestions,
    RemoveSearchSuggestions,
    SelectableText,
    ShowChangelogs,
    ResponseLogging,

    // ── Customize ────────────────────────────────────────────────────────────
    CustomFont,
    CustomEmojiFont,
    CustomPostFontSize,
    CustomNavBar,
    CustomSideBar,
    CustomTimelineTabs,
    CustomProfileTabs,
    CustomExploreTabs,
    CustomSearchTabs,
    CustomNotificationTabs,
    CustomInlineBar,
    CustomTypeAheadResponse,
    DefaultReplySorting,

    // ── Premium ──────────────────────────────────────────────────────────────
    EnableForcePip,
    EnableUndoPost,
    RedirectBMTab,

    // ── Share menu ───────────────────────────────────────────────────────────
    InlineDownloadButton,
    NativeDownloader,
    NativeReaderMode,
    NativeShareImage,
    NativeTranslator,
    BrowseTweetObject,
)
