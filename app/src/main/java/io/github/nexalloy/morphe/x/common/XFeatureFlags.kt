package io.github.nexalloy.morphe.x.common

/**
 * Registry for X feature-flag overrides.
 * Each feature-flag patch registers its mappings during patch setup;
 * FeatureFlagHookPatch consults this at runtime.
 */
object XFeatureFlags {
    private val overrides = mutableMapOf<String, Boolean>()

    fun register(flagName: String, value: Boolean) {
        overrides[flagName] = value
    }

    fun getOverride(flagName: String): Boolean? = overrides[flagName]
}
