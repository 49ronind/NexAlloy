package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch
import io.github.nexalloy.setObjectField

/** Hides Live Threads (Fleets successor) from timelines. */
val HideLiveThreads = patch(
    name = "Hide Live Threads",
    description = "Hides Live Threads from timelines.",
) {
    dependsOn(XVersionCheck)

    HideLiveThreadsFingerprint.hookMethod {
        after { param ->
            val response = param.result ?: return@after
            try {
                val listField = response.javaClass.declaredFields
                    .firstOrNull { it.type == java.util.ArrayList::class.java }
                    ?: return@after
                listField.isAccessible = true
                listField.set(response, null)
            } catch (_: Exception) {}
        }
    }
}
