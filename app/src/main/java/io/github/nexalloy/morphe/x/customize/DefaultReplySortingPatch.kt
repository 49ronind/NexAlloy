package io.github.nexalloy.morphe.x.customize

import io.github.nexalloy.morphe.x.common.XPref
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Changes the default reply-sort order.
 * Pref key: "x_default_reply_sort"  ("RELEVANCE" | "RECENCY")
 */
val DefaultReplySorting = patch(
    name = "Customize default reply sorting",
    description = "Sets the default reply-sort order (Relevance or Recency).",
    use = false,
) {
    dependsOn(XVersionCheck)

    DefaultReplySortingFingerprint.hookMethod {
        after { param ->
            val sort = XPref.getString("x_default_reply_sort").ifEmpty { return@after }
            // The return value is an enum or string; override with user preference.
            val result = param.result ?: return@after
            val cls = result.javaClass
            if (cls.isEnum) {
                @Suppress("UNCHECKED_CAST")
                val enumCls = cls as Class<out Enum<*>>
                val target = enumCls.enumConstants?.firstOrNull {
                    it.name.equals(sort, ignoreCase = true)
                } ?: return@after
                param.result = target
            } else if (result is String) {
                param.result = sort
            }
        }
    }
}
