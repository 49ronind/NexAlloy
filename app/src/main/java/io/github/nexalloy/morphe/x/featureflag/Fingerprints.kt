package io.github.nexalloy.morphe.x.featureflag

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint

/**
 * The class that owns all X/Twitter feature switches.
 * Identified by the unique config string it references.
 */
private object FeatureFlagClassFingerprint : Fingerprint(
    strings = listOf("feature_switches_configs_crashlytics_enabled"),
)

/**
 * boolean getFlag(String flagName, boolean defaultValue)
 * – the core boolean lookup inside the feature-flag class.
 */
internal object FeatureFlagBooleanMethodFingerprint : Fingerprint(
    classFingerprint = FeatureFlagClassFingerprint,
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;", "Z"),
)
