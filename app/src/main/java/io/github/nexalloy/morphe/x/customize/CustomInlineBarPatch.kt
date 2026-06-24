package io.github.nexalloy.morphe.x.customize

import io.github.nexalloy.morphe.x.common.XPref
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

@Suppress("UNCHECKED_CAST")
private fun filterList(list: Any?, toRemove: Set<String>): Any? {
    if (toRemove.isEmpty()) return list
    val mutable = (list as? MutableList<Any?>) ?: return list
    mutable.removeAll { it?.toString()?.let { s -> toRemove.contains(s) } == true }
    return mutable
}

/**
 * Hides buttons from the inline action bar shown below each post
 * (reply, repost, like, share, bookmark…).
 * Pref key: "x_hide_inline_bar_items"
 */
val CustomInlineBar = patch(
    name = "Customize Inline action Bar",
    description = "Hides buttons from the inline action bar below posts.",
    use = false,
) {
    dependsOn(XVersionCheck)

    InlineBarBuilderFingerprint.hookMethod {
        after { param ->
            val hidden = XPref.getStringSet("x_hide_inline_bar_items")
            param.result = filterList(param.result, hidden)
        }
    }
}
