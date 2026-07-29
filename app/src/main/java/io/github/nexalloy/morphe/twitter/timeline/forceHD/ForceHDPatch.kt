package io.github.nexalloy.morphe.twitter.timeline.forceHD

import io.github.nexalloy.patch

val ForceHD = patch(
    name = "Enable force HD videos",
    description = "Videos will be played in the highest quality available.",
) {
    val videoListField = ::playerSupportVideoListFieldResolved.field.also { it.isAccessible = true }

    PlayerSupportFingerprint.hookMethod {
        before { param ->
            for (arg in param.args) {
                if (arg == null) continue
                val currentList = runCatching { videoListField.get(arg) as? List<*> }.getOrNull()
                    ?: continue
                runCatching { videoListField.set(arg, timelineVideos(currentList)) }
                break
            }
        }
    }
}
