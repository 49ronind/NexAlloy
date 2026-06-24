package io.github.nexalloy.morphe.x.customize

import io.github.nexalloy.morphe.x.common.XPref
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Filters type-ahead (search-as-you-type) suggestions.
 * Pref key: "x_hide_typeahead_types" (set of suggestion type strings to hide,
 *            e.g. "trending_queries", "recent_searches")
 */
val CustomTypeAheadResponse = patch(
    name = "Customize search suggestions",
    description = "Filters type-ahead search suggestions by category.",
    use = false,
) {
    dependsOn(XVersionCheck)

    TypeAheadResponseFingerprint.hookMethod {
        after { param ->
            val hidden = XPref.getStringSet("x_hide_typeahead_types")
            if (hidden.isEmpty()) return@after
            try {
                @Suppress("UNCHECKED_CAST")
                val list = (param.result as? MutableList<Any?>) ?: return@after
                list.removeAll { item ->
                    if (item == null) return@removeAll false
                    val typeField = item.javaClass.declaredFields
                        .firstOrNull { it.type == String::class.java }
                        ?.also { it.isAccessible = true }
                    val type = typeField?.get(item) as? String ?: return@removeAll false
                    hidden.contains(type)
                }
            } catch (_: Exception) {}
        }
    }
}
