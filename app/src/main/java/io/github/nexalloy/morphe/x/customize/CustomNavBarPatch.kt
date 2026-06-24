package io.github.nexalloy.morphe.x.customize

import io.github.nexalloy.morphe.x.common.XPref
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.morphe.x.common.x_is_11_88_or_greater
import io.github.nexalloy.morphe.x.featureflag.NavBarFix
import io.github.nexalloy.patch

@Suppress("UNCHECKED_CAST")
private fun filterList(list: Any?, toRemove: Set<String>): Any? {
    if (toRemove.isEmpty()) return list
    val mutable = (list as? MutableList<Any?>) ?: return list
    mutable.removeAll { it?.toString()?.let { s -> toRemove.contains(s) } == true }
    return mutable
}

/**
 * Hides/reorders navigation bar tabs.
 * Pref key: "x_hide_nav_items" (String set of tab toString() values to hide)
 */
val CustomNavBar = patch(
    name = "Customize Navigation Bar",
    description = "Hides or reorders navigation bar tabs.",
    use = false,
) {
    dependsOn(XVersionCheck, NavBarFix)

    val fp = if (x_is_11_88_or_greater) NavBarBuilder2Fingerprint else NavBarBuilderFingerprint

    fp.hookMethod {
        after { param ->
            val hidden = XPref.getStringSet("x_hide_nav_items")
            param.result = filterList(param.result, hidden)
        }
    }
}
