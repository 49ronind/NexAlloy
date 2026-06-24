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
 * Hides tabs from the home-timeline top bar (For You, Following…).
 * Pref key: "x_hide_timeline_tabs"
 */
val CustomTimelineTabs = patch(
    name = "Customize timeline top bar",
    description = "Hides tabs from the home timeline top bar.",
    use = false,
) {
    dependsOn(XVersionCheck)

    TimelineTabsBuilderFingerprint.hookMethod {
        after { param ->
            val hidden = XPref.getStringSet("x_hide_timeline_tabs")
            param.result = filterList(param.result, hidden)
        }
    }
}
