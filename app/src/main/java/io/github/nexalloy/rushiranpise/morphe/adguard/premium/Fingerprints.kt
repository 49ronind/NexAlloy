package io.github.nexalloy.rushiranpise.morphe.adguard.premium

import io.github.nexalloy.morphe.Fingerprint

// Class anchors
private val PlusManagerClassFingerprint = Fingerprint(
    strings = listOf("Failed to get state from backend. Remaining retry count: "),
)

private val PaidLicenseClassFingerprint = Fingerprint(
    strings = listOf("PaidLicense(licenseKey="),
)

// Method fingerprints
object GetPlusStateFingerprint : Fingerprint(
    classFingerprint = PlusManagerClassFingerprint,
    returnType = "L",
    parameters = listOf(),
)

object SetPlusStateFingerprint : Fingerprint(
    classFingerprint = PlusManagerClassFingerprint,
    returnType = "V",
)

object StateFlowResolverFingerprint : Fingerprint(
    classFingerprint = PlusManagerClassFingerprint,
    returnType = "L",
)

object PromoStateFlowResolverFingerprint : Fingerprint(
    classFingerprint = PlusManagerClassFingerprint,
    returnType = "L",
    strings = listOf("cacheStrategy", "retryStrategy"),
)

object PaidLicenseFingerprint : Fingerprint(
    classFingerprint = PaidLicenseClassFingerprint,
    name = "<init>",
)

object LifetimeDurationFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("Lifetime"),
)

object LicenseKeyActivateFingerprint : Fingerprint(
    classFingerprint = PlusManagerClassFingerprint,
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)
