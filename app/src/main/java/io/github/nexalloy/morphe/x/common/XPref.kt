package io.github.nexalloy.morphe.x.common

import de.robv.android.xposed.XSharedPreferences
import io.github.nexalloy.BuildConfig

/**
 * Runtime preferences accessor for X patches.
 * Call [init] once during patch setup (PatchExecutor context).
 */
object XPref {
    private var prefs: XSharedPreferences? = null

    fun init(packageName: String) {
        prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, packageName)
            .takeIf { it.file.canRead() }
    }

    private fun reload() = prefs?.reload()

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        reload(); return prefs?.getBoolean(key, default) ?: default
    }

    fun getString(key: String, default: String = ""): String {
        reload(); return prefs?.getString(key, default) ?: default
    }

    fun getStringSet(key: String): Set<String> {
        reload(); return prefs?.getStringSet(key, emptySet()) ?: emptySet()
    }
}
