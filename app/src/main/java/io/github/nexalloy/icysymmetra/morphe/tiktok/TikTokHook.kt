package io.github.nexalloy.icysymmetra.morphe.tiktok

import io.github.nexalloy.Patch
import io.github.nexalloy.icysymmetra.morphe.tiktok.ad.FeedFilter
import io.github.nexalloy.icysymmetra.morphe.tiktok.download.WatermarkFreeDownloads
import io.github.nexalloy.icysymmetra.morphe.tiktok.interaction.InteractionEnhancements
import io.github.nexalloy.icysymmetra.morphe.tiktok.login.DisableForcedLogin
import io.github.nexalloy.icysymmetra.morphe.tiktok.misc.SanitizeShareUrls
import io.github.nexalloy.icysymmetra.morphe.tiktok.misc.SimSpoof

val TikTokPatches = arrayOf<Patch>(
    WatermarkFreeDownloads,
    FeedFilter,
    DisableForcedLogin,
    InteractionEnhancements,
    SimSpoof,
    SanitizeShareUrls,
)
