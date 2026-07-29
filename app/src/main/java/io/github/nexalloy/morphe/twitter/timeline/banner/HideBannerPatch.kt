package io.github.nexalloy.morphe.twitter.timeline.banner

import io.github.nexalloy.patch

/**
 * NOTE: fingerprint only narrows down by class + boolean return type
 * (same as upstream piko, which additionally anchors on a RETURN opcode
 * inside the method). If this class exposes more than one matching
 * boolean method, this may need a stricter fingerprint - please verify
 * the "new posts" banner is actually hidden when testing.
 */
val HideBanner = patch(
    name = "Hide Banner",
    description = "Hides the \"new posts\" banner shown at the top of the timeline.",
) {
    HideBannerFingerprint.hookMethod {
        after { param -> param.result = false }
    }
}
