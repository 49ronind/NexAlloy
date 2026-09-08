package io.github.nexalloy.rushiranpise.morphe.dramabox.lock

import android.app.Activity
import android.content.Context
import io.github.nexalloy.patch

val ActivationLockBypass = patch(
    name = "Bypass activation lock",
    description = "Bypasses activation code lock screen on repackaged DramaBox releases.",
) {
    runCatching {
        SplashLockOnCreateFingerprint.hookMethod {
            before { param ->
                val activity = param.thisObject as? Activity ?: return@before
                activity.getSharedPreferences("drama_lock_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_verified", true)
                    .commit()
            }
        }
    }
}
