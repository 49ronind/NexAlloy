package io.github.nexalloy.revanced.facebook.ad

import io.github.nexalloy.patch
import io.github.nexalloy.revanced.facebook.hookReelsInstreamAdBreakParser

val HideFacebookAdsExtra = patch(
    name = "Hide Facebook ads (Reels ad break)",
    description = "Chặn ad break trong Reels tại parser X.5Jc.A01(GraphQLMedia).",
) {

    runCatching {
        hookReelsInstreamAdBreakParser(
            ::reelsInstreamAdBreakParserFingerprint.method,
            classLoader,
        )
    }
}
