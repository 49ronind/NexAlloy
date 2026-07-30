package io.github.nexalloy.morphe.twitter.timeline.live

import app.morphe.extension.shared.Logger
import io.github.nexalloy.patch
import io.github.nexalloy.setObjectField

val HideLiveThreads = patch(
    name = "Hide Live Threads",
    description = "Hides live threads (fleets-style) from the timeline.",
) {
    val field = runCatching {
        ::hideLiveThreadsFieldResolved.field.also { it.isAccessible = true }
    }.getOrElse { e ->
        Logger.printException({ "[Twitter] HideLiveThreads: failed to resolve field" }, e)
        return@patch
    }

    HideLiveThreadsFingerprint.hookMethod {
        after { param ->
            val instance = param.thisObject ?: return@after
            instance.setObjectField(field.name, null)
        }
    }
}

