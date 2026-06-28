package io.github.nexalloy.morphe.twitter.utils

import io.github.nexalloy.patch
import kotlin.properties.Delegates

var is_11_70_or_greater: Boolean by Delegates.notNull()
    private set

var is_11_82_or_greater: Boolean by Delegates.notNull()
    private set

var is_11_88_or_greater: Boolean by Delegates.notNull()
    private set

var is_11_92_or_greater: Boolean by Delegates.notNull()
    private set

var is_11_40_or_greater: Boolean by Delegates.notNull()
    private set

val VersionCheck = patch(name = "<VersionCheck>") {
    val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toInt()
    } else {
        @Suppress("DEPRECATION") packageInfo.versionCode
    }

    fun isEqualsOrGreaterThan(version: Int): Boolean = versionCode >= version

    is_11_70_or_greater = isEqualsOrGreaterThan(311700000)
    is_11_82_or_greater = versionCode == 311820000 || isEqualsOrGreaterThan(311830000)
    is_11_88_or_greater = isEqualsOrGreaterThan(311880000)
    is_11_92_or_greater = isEqualsOrGreaterThan(311920000)
    is_11_40_or_greater = isEqualsOrGreaterThan(311400000)
}
