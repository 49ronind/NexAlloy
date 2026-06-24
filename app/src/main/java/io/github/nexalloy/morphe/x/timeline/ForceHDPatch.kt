package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Forces videos to play at the highest available quality.
 * Mirrors piko's TimelineEntry.timelineVideos(): keeps only the max-bitrate variant.
 */
val ForceHD = patch(
    name = "Enable force HD videos",
    description = "Forces videos to always play at the highest available quality.",
) {
    dependsOn(XVersionCheck)

    PlayerSupportFingerprint.hookMethod {
        after { param ->
            try {
                @Suppress("UNCHECKED_CAST")
                val variants = param.result as? List<Any?> ?: return@after
                if (variants.size <= 1) return@after

                var maxBitrate = -1
                var maxVariant: Any? = null

                for (v in variants) {
                    if (v == null) continue
                    val cls = v.javaClass
                    // Find bitrate field (int) and url field (String)
                    val bitrateField = cls.declaredFields
                        .firstOrNull { it.type == Int::class.javaPrimitiveType
                                    && !it.name.contains("type", true) }
                    val extField = cls.declaredFields
                        .firstOrNull { it.type == String::class.java }

                    bitrateField?.isAccessible = true
                    extField?.isAccessible = true

                    val ext = extField?.get(v) as? String ?: continue
                    if (!ext.equals("mp4", true)) continue

                    val bitrate = bitrateField?.getInt(v) ?: 0
                    if (bitrate >= maxBitrate) {
                        maxBitrate = bitrate
                        maxVariant = v
                    }
                }

                if (maxVariant != null) {
                    param.result = listOf(maxVariant)
                }
            } catch (_: Exception) {}
        }
    }
}
