package io.github.nexalloy.morphe.x.common

import io.github.nexalloy.patch
import kotlin.properties.Delegates

var x_is_11_40_or_greater: Boolean by Delegates.notNull(); private set
var x_is_11_70_or_greater: Boolean by Delegates.notNull(); private set
var x_is_11_88_or_greater: Boolean by Delegates.notNull(); private set
var x_is_11_92_or_greater: Boolean by Delegates.notNull(); private set
var x_is_11_95_or_greater: Boolean by Delegates.notNull(); private set

/**
 * Initialises [XPref] and version flags.
 * Must be first entry in [XPatches].
 */
val XVersionCheck = patch {
    XPref.init(lpparam.packageName)

    @Suppress("DEPRECATION")
    val vc = appContext.packageManager
        .getPackageInfo(lpparam.packageName, 0).versionCode

    fun gte(v: Int) = vc >= v
    x_is_11_40_or_greater = gte(311_400_000)
    x_is_11_70_or_greater = gte(311_700_000)
    x_is_11_88_or_greater = gte(311_880_000)
    x_is_11_92_or_greater = gte(311_920_000)
    x_is_11_95_or_greater = gte(311_950_000)
}
