package io.github.nexalloy.morphe.x.customize

import io.github.nexalloy.morphe.x.common.XPref
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Overrides the post body text size.
 * Pref key: "x_post_font_size"  (float, default 15.0)
 */
val CustomPostFontSize = patch(
    name = "Customise post font size",
    description = "Changes the font size of post body text.",
    use = false,
) {
    dependsOn(XVersionCheck)

    PostFontSizeFingerprint.hookMethod {
        after { param ->
            val size = XPref.getString("x_post_font_size").toFloatOrNull() ?: return@after
            if (size > 0f) param.result = size
        }
    }
}
