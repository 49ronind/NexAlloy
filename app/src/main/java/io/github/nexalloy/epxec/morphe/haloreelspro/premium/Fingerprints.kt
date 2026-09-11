package io.github.nexalloy.epxec.morphe.haloreelspro.premium

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint

object HaloreelsproVipFingerprint : Fingerprint(
    name = "isVip",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
)
