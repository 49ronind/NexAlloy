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

/** Hides tabs from profile pages (Tweets, Replies, Media, Likes). */
val CustomProfileTabs = patch(
    name = "Customize profile tabs",
    description = "Hides tabs from profile pages.",
    use = false,
) {
    dependsOn(XVersionCheck)

    ProfileTabsBuilderFingerprint.hookMethod {
        after { param ->
            val hidden = XPref.getStringSet("x_hide_profile_tabs")
            param.result = filterList(param.result, hidden)
        }
    }
}
