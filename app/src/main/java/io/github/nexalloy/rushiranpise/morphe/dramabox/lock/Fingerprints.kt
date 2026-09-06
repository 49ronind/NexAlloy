package io.github.nexalloy.rushiranpise.morphe.dramabox.lock

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.string

/**
 * Targets SplashLockActivity.onCreate(Bundle)V
 * Package: com.storymatrix.drama.lock.SplashLockActivity
 *
 * This launcher activity presents an activation code lock screen on repackaged
 * distributions (e.g. Uptodown / third-party mod repack).
 */
object SplashLockOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/storymatrix/drama/lock/SplashLockActivity;",
    name = "onCreate",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PROTECTED),
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        string("drama_lock_prefs"),
        string("is_verified"),
    ),
)
