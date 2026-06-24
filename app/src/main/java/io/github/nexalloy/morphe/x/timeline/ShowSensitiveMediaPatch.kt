package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Automatically shows sensitive media without requiring a tap-through.
 * Mirrors piko's sensitiveMedia(): sets the three boolean flags to false.
 */
val ShowSensitiveMedia = patch(
    name = "Show sensitive media",
    description = "Automatically shows sensitive media without a warning overlay.",
) {
    dependsOn(XVersionCheck)

    SensitiveMediaFingerprint.hookMethod {
        after { param ->
            val warning = param.result ?: return@after
            try {
                val cls = warning.javaClass
                val boolFields = cls.declaredFields
                    .filter { it.type == Boolean::class.javaPrimitiveType }
                // piko sets fields a, b, c to false
                for (i in 0 until minOf(3, boolFields.size)) {
                    boolFields[i].also { it.isAccessible = true }.setBoolean(warning, false)
                }
            } catch (_: Exception) {}
        }
    }
}
