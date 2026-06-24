package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Hides community membership badges shown on avatars.
 * Forces the membership role field to NON_MEMBER.
 */
val HideCommunityBadge = patch(
    name = "Hide community badges",
    description = "Hides community membership badges on user avatars.",
) {
    dependsOn(XVersionCheck)

    CommunityModelFingerprint.hookMethod {
        after { param ->
            try {
                val result = param.result ?: return@after
                val cls = result.javaClass
                // The role field is the last IPUT_OBJECT target; find enum field containing role
                val roleField = cls.declaredFields.lastOrNull { it.type.isEnum }
                    ?: return@after
                roleField.isAccessible = true
                // Find NON_MEMBER enum constant
                @Suppress("UNCHECKED_CAST")
                val enumCls = roleField.type as Class<out Enum<*>>
                val nonMember = enumCls.enumConstants
                    ?.firstOrNull { it.name.contains("NON_MEMBER", ignoreCase = true) }
                    ?: return@after
                roleField.set(result, nonMember)
            } catch (_: Exception) {}
        }
    }
}
