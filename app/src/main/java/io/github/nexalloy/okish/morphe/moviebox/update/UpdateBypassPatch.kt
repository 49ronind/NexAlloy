package io.github.nexalloy.okish.morphe.moviebox.update

import io.github.nexalloy.patch

val UpdateBypass = patch(
    name = "Disable update prompts",
    description = "Disables forced and nag update prompts in MovieBox.",
) {
    ForceUpdateFingerprint.hookMethod {
        before { param ->
            param.result = false
        }
    }

    HasUpdateFingerprint.hookMethod {
        before { param ->
            param.result = false
        }
    }
}
