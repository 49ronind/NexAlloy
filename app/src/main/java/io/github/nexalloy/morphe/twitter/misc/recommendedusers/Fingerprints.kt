package io.github.nexalloy.morphe.twitter.misc.recommendedusers

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint

/**
 * Model class name is used for JSON deserialization and is not obfuscated,
 * so we can target its constructor directly.
 */
internal object HideRecommendedUsersFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/people/JsonProfileRecommendationModuleResponse;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
)
