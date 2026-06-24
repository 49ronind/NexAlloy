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
 * Hides items from the navigation drawer (side bar).
 * Pref key: "x_hide_sidebar_items"
 */
val CustomSideBar = patch(
    name = "Customize side bar items",
    description = "Hides items from the navigation drawer.",
    use = false,
) {
    dependsOn(XVersionCheck)

    SideBarBuilderFingerprint.hookMethod {
        after { param ->
            val hidden = XPref.getStringSet("x_hide_sidebar_items")
            param.result = filterList(param.result, hidden)
        }
    }
}
