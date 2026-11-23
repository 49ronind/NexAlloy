package io.github.nexalloy.bholeykabhakt.morphe.mtmanager.vip

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private const val STARTUP_GRACE_MS = 8001L
private const val DIALOG_CLOSE_GRACE_MS = 400L
private const val VIP_EXPIRY_MS = 4102444800000L // 2100-01-01T00:00:00Z

val UnlockVip = patch(
    name = "Unlock VIP",
    description = "Unlocks client-side VIP features in MT Manager (some features remain server-side locked).",
) {
    val startMs = SystemClock.uptimeMillis()
    val dialogDepth = AtomicInteger(0)
    val lastDialogCloseMs = AtomicLong(0L)
    val netStorageDepth = ThreadLocal<Int>()
    val nativeHooksInstalled = AtomicBoolean(false)

    fun shouldForceVip(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - startMs < STARTUP_GRACE_MS) return false
        if (dialogDepth.get() > 0) return true
        val closedAt = lastDialogCloseMs.get()
        return closedAt != 0L && (now - closedAt) < DIALOG_CLOSE_GRACE_MS
    }

    fun onDialogEvent(event: Int) {
        when {
            event >= 1 -> dialogDepth.incrementAndGet()
            event == 0 -> dialogDepth.set(0)
            else -> {
                dialogDepth.updateAndGet { current -> if (current >= 1) current - 1 else 0 }
                lastDialogCloseMs.set(SystemClock.uptimeMillis())
            }
        }
    }

    fun ensureNativeVipHooks() {
        if (!nativeHooksInstalled.compareAndSet(false, true)) return
        runCatching {
            val vipClass = Class.forName(VIP_NATIVE_CLASS, true, classLoader)

            vipClass.declaredMethods.first {
                it.name == VIP_BOOL_A && it.parameterTypes.isEmpty() && it.returnType == Boolean::class.javaPrimitiveType
            }.hookMethod {
                before { param ->
                    if (shouldForceVip()) {
                        param.result = true
                    }
                }
            }

            vipClass.declaredMethods.first {
                it.name == IS_VIP && it.parameterTypes.isEmpty() && it.returnType == Boolean::class.javaPrimitiveType
            }.hookMethod {
                before { param ->
                    if ((netStorageDepth.get() ?: 0) > 0 || shouldForceVip()) {
                        param.result = true
                    }
                }
            }

            vipClass.declaredMethods.first {
                it.name == VIP_EXPIRY && it.parameterTypes.isEmpty() && it.returnType == Long::class.javaPrimitiveType
            }.hookMethod {
                before { param ->
                    if (shouldForceVip()) {
                        param.result = VIP_EXPIRY_MS
                    }
                }
            }
        }.onFailure { err ->
            nativeHooksInstalled.set(false)
            XposedBridge.log(err)
        }
    }

    // Install native VIP hooks on first Activity resume (after libmtprotect.so RegisterNatives completes).
    appContext.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) = ensureNativeVipHooks()
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    })

    // Bridge MT's dialog lifecycle so VIP is forced only around dialog windows (+ 400ms close grace).
    fun hookDialogEvent(className: String, methodName: String, event: Int) {
        runCatching {
            val clazz = classLoader.loadClass(className)
            clazz.getDeclaredMethod(methodName).hookMethod {
                before {
                    if (event >= 0) ensureNativeVipHooks()
                    onDialogEvent(event)
                }
            }
        }.onFailure { XposedBridge.log(it) }
    }

    hookDialogEvent(DIALOG_A_CLASS, "onStart", 1)
    hookDialogEvent(DIALOG_A_CLASS, "onStop", -1)
    hookDialogEvent(DIALOG_B_CLASS, "onStart", 1)
    hookDialogEvent(DIALOG_B_CLASS, "dismiss", -1)
    hookDialogEvent(MAIN_ACTIVITY_CLASS, "onResume", 0)

    // Network-storage protocol chooser is a SubMenu (not a Dialog): force isVip() within its scope.
    runCatching {
        NetStorageChooserFingerprint.hookMethod {
            before {
                ensureNativeVipHooks()
                netStorageDepth.set((netStorageDepth.get() ?: 0) + 1)
            }
            after {
                val next = (netStorageDepth.get() ?: 0) - 1
                if (next <= 0) netStorageDepth.remove() else netStorageDepth.set(next)
            }
        }
    }.onFailure { XposedBridge.log(it) }

    // While VIP is forced, account-status subtitle may carry string res id -1: return fallback text instead.
    runCatching {
        val subtitleClass = classLoader.loadClass(SUBTITLE_CLASS)
        val subtitleResField = subtitleClass.getDeclaredField(SUBTITLE_RES).apply { isAccessible = true }
        val subtitleTextField = subtitleClass.getDeclaredField(SUBTITLE_TEXT).apply { isAccessible = true }
        subtitleClass.declaredMethods.first {
            it.name == SUBTITLE_RES && it.returnType == String::class.java && it.parameterTypes.isEmpty()
        }.hookMethod {
            before { param ->
                if (subtitleResField.getInt(param.thisObject) < 0) {
                    param.result = subtitleTextField.get(param.thisObject) as? String
                }
            }
        }
    }.onFailure { XposedBridge.log(it) }
}
