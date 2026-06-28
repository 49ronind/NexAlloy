package io.github.nexalloy.morphe.twitter

import io.github.nexalloy.Patch
import io.github.nexalloy.morphe.twitter.ads.timelineEntryHook.HideAds
import io.github.nexalloy.morphe.twitter.ads.timelineEntryHook.HideRecommendationItems
import io.github.nexalloy.morphe.twitter.featureFlag.DisableChirpFont
import io.github.nexalloy.morphe.twitter.link.cleartrackingparams.ClearTrackingParams
import io.github.nexalloy.morphe.twitter.link.unshorten.NoShortenedUrl
import io.github.nexalloy.morphe.twitter.misc.blockRedirectToXLite.BlockRedirectingToXLite
import io.github.nexalloy.morphe.twitter.timeline.disableAutoScroll.DisableAutoScroll
import io.github.nexalloy.morphe.twitter.timeline.forceHD.ForceHD
import io.github.nexalloy.morphe.twitter.timeline.removePremiumUpsell.RemovePremiumUpsell
import io.github.nexalloy.morphe.twitter.timeline.sensitivemediasettings.ShowSensitiveMedia
import io.github.nexalloy.morphe.twitter.timeline.showpollresults.ShowPollResults


val TwitterPatches: Array<Patch> = arrayOf(
    // Ads / recommendations
    HideAds,
    HideRecommendationItems,

    // Feature-flag driven toggles
    DisableChirpFont,

    // Links
    ClearTrackingParams,
    NoShortenedUrl,

    // Misc
    BlockRedirectingToXLite,

    // Timeline / tweet UI
    DisableAutoScroll,
    ForceHD,
    RemovePremiumUpsell,
    ShowSensitiveMedia,
    ShowPollResults,

)
