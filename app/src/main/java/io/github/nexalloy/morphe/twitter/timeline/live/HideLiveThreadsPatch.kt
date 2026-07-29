package io.github.nexalloy.morphe.twitter.timeline.live

import io.github.nexalloy.findFirstFieldByExactTypeOrNull
import io.github.nexalloy.patch
import io.github.nexalloy.setObjectField

val HideLiveThreads = patch(
    name = "Hide Live Threads",
    description = "Hides live threads (fleets-style) from the timeline.",
) {
    HideLiveThreadsFingerprint.hookMethod {
        after { param ->
            val instance = param.thisObject ?: return@after
            val listField = instance.javaClass.findFirstFieldByExactTypeOrNull(ArrayList::class.java)
                ?: return@after
            instance.setObjectField(listField.name, null)
        }
    }
}
