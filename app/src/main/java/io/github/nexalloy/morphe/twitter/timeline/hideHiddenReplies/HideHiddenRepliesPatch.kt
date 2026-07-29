package io.github.nexalloy.morphe.twitter.timeline.hideHiddenReplies

import io.github.nexalloy.patch

val HideHiddenReplies = patch(
    name = "Hide hidden replies",
    description = "Hides the \"hidden replies\" indicator/entry point on tweets.",
) {
    HideHiddenRepliesFingerprint.hookMethod {
        after { param ->
            val instance = param.thisObject ?: return@after
            runCatching {
                val boolField = instance.javaClass.declaredFields
                    .lastOrNull { it.type == Boolean::class.javaPrimitiveType }
                    ?: return@after
                boolField.isAccessible = true
                boolField.setBoolean(instance, false)
            }
        }
    }
}
