package io.github.nexalloy.sysadmindoc.hushfeed.tiktok

import io.github.nexalloy.Patch
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.download.WatermarkFreeDownloads
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.feed.FeedAdFilter
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.feed.HideLiveVideos
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.feed.HideShopVideos
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.feed.HideStories
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.feed.HideTakoAi
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.interaction.AllowScreenCapture
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.interaction.DisableLongPressQuickShare
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.interaction.DisableLongPressRepost
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.interaction.DisableScreenCaptureDetection
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.login.DisableForcedLogin
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.misc.CopyCommentsWithoutUsername
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.misc.OpenExternalLinks
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.misc.SanitizeShareUrls
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.misc.SimSpoof
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.playback.ShowSeekbar
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.playback.StopVideoLooping
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.BlockAdvertisingId
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.BlockClipboardReads
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.BlockContactListAccess
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.BlockInstalledAppScanning
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.BrowserPrivacyGuard
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.DisableTelemetry
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.GhostMode
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.HideCaptchaPopups
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.HideReadReceipts
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.HideVpnConnection
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.KeepUnsentMessages
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.LocationAccessGovernor
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.StopSearchHistory
import io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy.StopWatchHistory

val TikTokPatches: Array<Patch> = arrayOf(
    // Core defaults (enabled by default)
    WatermarkFreeDownloads,
    FeedAdFilter,
    DisableForcedLogin,
    ShowSeekbar,
    DisableScreenCaptureDetection,
    DisableLongPressRepost,
    SanitizeShareUrls,
    CopyCommentsWithoutUsername,
    OpenExternalLinks,
    SimSpoof,
    // Opt-in HushFeed controls (disabled by default)
    HideLiveVideos,
    HideShopVideos,
    HideStories,
    HideTakoAi,
    StopVideoLooping,
    DisableLongPressQuickShare,
    AllowScreenCapture,
    DisableTelemetry,
    HideCaptchaPopups,
    GhostMode,
    HideReadReceipts,
    // Opt-in HushFeed Privacy Pack (disabled by default)
    BlockContactListAccess,
    BlockInstalledAppScanning,
    LocationAccessGovernor,
    BlockClipboardReads,
    BlockAdvertisingId,
    HideVpnConnection,
    StopWatchHistory,
    StopSearchHistory,
    BrowserPrivacyGuard,
    KeepUnsentMessages,
)
