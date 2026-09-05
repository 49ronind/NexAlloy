package io.github.nexalloy.revanced.spotify

import io.github.nexalloy.Patch
import io.github.nexalloy.revanced.spotify.ads.InterceptAds
import io.github.nexalloy.revanced.spotify.premium.UnlockPremium
import io.github.nexalloy.revanced.spotify.privacy.SanitizeSharingLinks
import io.github.nexalloy.revanced.spotify.session.SessionProtection

val SpotifyPatches = arrayOf<Patch>(
    InterceptAds,
    SessionProtection,
    UnlockPremium,
    SanitizeSharingLinks,
)
