package io.github.nexalloy.morphe.x.sharemenu

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Enables the native in-app Translator button in the tweet share menu.
 * Mirrors piko's NativeTranslatorPatch.
 */
val NativeTranslator = patch(
    name = "Native translator",
    description = "Enables the in-app translator option in the tweet share menu.",
) {
    dependsOn(XVersionCheck)

    TranslatorFingerprint.hookMethod {
        after { param ->
            if (param.result is Boolean) param.result = true
        }
    }
}
