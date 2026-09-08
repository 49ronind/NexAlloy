package io.github.nexalloy.rushiranpise.morphe.mangaplus.premium

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint

object SubscriptionPlanDeserializerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("deluxe", "standard"),
)
