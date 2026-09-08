package io.github.nexalloy.okish.morphe.moviebox

import io.github.nexalloy.Patch
import io.github.nexalloy.okish.morphe.moviebox.ad.AdRemoval
import io.github.nexalloy.okish.morphe.moviebox.download.ParallelDownloads
import io.github.nexalloy.okish.morphe.moviebox.premium.PremiumUnlock
import io.github.nexalloy.okish.morphe.moviebox.update.UpdateBypass

val MovieBoxPatches = arrayOf<Patch>(
    UpdateBypass,
    ParallelDownloads,
    AdRemoval,
    PremiumUnlock,
)
