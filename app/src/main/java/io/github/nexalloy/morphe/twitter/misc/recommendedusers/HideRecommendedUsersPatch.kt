package io.github.nexalloy.morphe.twitter.misc.recommendedusers

import io.github.nexalloy.findFirstFieldByExactTypeOrNull
import io.github.nexalloy.patch
import io.github.nexalloy.setObjectField

val HideRecommendedUsers = patch(
    name = "Hide recommended users",
    description = "Hides recommended users that pop up when you follow someone.",
) {
    HideRecommendedUsersFingerprint.hookMethod {
        after { param ->
            val instance = param.thisObject ?: return@after
            val listField = instance.javaClass.findFirstFieldByExactTypeOrNull(ArrayList::class.java)
                ?: return@after
            instance.setObjectField(listField.name, null)
        }
    }
}
