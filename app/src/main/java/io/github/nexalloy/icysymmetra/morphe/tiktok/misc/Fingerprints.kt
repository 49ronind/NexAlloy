package io.github.nexalloy.icysymmetra.morphe.tiktok.misc

import io.github.nexalloy.morphe.Fingerprint

object ShareUrlTrackerFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("utm_campaign", "share_link_id"),
)
