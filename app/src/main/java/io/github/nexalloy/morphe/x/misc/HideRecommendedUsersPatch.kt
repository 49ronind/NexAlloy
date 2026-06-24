package io.github.nexalloy.morphe.x.misc

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Hides the "recommended users" pop-up that appears after following someone.
 * Mirrors piko: returns null for the users ArrayList.
 */
val HideRecommendedUsers = patch(
    name = "Hide Recommended Users",
    description = "Hides the recommended users pop-up shown after following someone.",
) {
    dependsOn(XVersionCheck)

    HideRecommendedUsersFingerprint.hookMethod {
        after { param ->
            try {
                val result = param.result ?: return@after
                val listField = result.javaClass.declaredFields
                    .firstOrNull { java.util.ArrayList::class.java.isAssignableFrom(it.type) }
                    ?: return@after
                listField.isAccessible = true
                listField.set(result, null)
            } catch (_: Exception) {}
        }
    }
}
