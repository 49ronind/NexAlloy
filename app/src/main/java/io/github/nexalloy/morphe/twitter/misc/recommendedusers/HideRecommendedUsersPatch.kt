package io.github.nexalloy.morphe.twitter.misc.recommendedusers

import app.morphe.extension.shared.Logger
import io.github.nexalloy.patch
import io.github.nexalloy.setObjectField

val HideRecommendedUsers = patch(
    name = "Hide recommended users",
    description = "Hides recommended users that pop up when you follow someone.",
) {
    HideRecommendedUsersFingerprint.hookMethod {
        before { param ->
            val instance = param.thisObject ?: return@before
            runCatching {
                instance.setObjectField("d", null)
            }.onFailure { e ->
                Logger.printException({ "[Twitter] HideRecommendedUsers: failed to set field" }, e)
            }
        }
    }
}

