package io.github.nexalloy.revanced.instagram.ghost.markasread

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.patch

private const val GHOST_BTN_TAG = "ie_ghost_seen_btn"

// Cached once on first use — resource IDs are constant for a given app install.
@Volatile private var sCachedComposerContainerId = 0

/**
 * Ghost DM Mark As Read
 *
 *
 * Hooks [View.onAttachedToWindow] and, when the attached view is
 * `row_thread_composer_buttons_container`, injects a ghost 👻 [ImageButton]
 * into that container's parent [ViewGroup]. Tapping the button scrolls the
 * `message_list` to the bottom (triggering Instagram's built-in mark-as-read)
 * and then scrolls back, simulating a read without emitting a read receipt.
 *
 * Note: InstaEclipse's original implementation used a module icon loaded via
 * [XModuleResources]. Because NexAlloy doesn't expose a module-resource path
 * at patch time, we fall back to the system `ic_menu_view` drawable and tint
 * it white — identical to InstaEclipse's own fallback branch.
 */
val GhostDMMarkAsRead = patch(
    name = "Ghost DM mark as read",
    description = "Injects a ghost button into the DM composer bar. " +
            "Tapping it silently marks the conversation as read while Ghost Mode is enabled.",
) {
    try {
        XposedHelpers.findAndHookMethod(
            View::class.java,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val view = param.thisObject as? View ?: return

                    // Resolve composer container ID once.
                    if (sCachedComposerContainerId == 0) {
                        @SuppressLint("DiscouragedApi")
                        val id = view.context.resources.getIdentifier(
                            "row_thread_composer_buttons_container",
                            "id",
                            view.context.packageName
                        )
                        sCachedComposerContainerId = id
                    }

                    if (sCachedComposerContainerId == 0 ||
                        view.id != sCachedComposerContainerId
                    ) return

                    val parent = view.parent as? ViewGroup ?: return
                    injectGhostButton(parent)
                }
            }
        )
    } catch (t: Throwable) {
        Logger.printException({ "GhostDMMarkAsRead hook failed" }, t)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun injectGhostButton(parent: ViewGroup) {
    // Guard: don't inject more than once per parent.
    if (parent.findViewWithTag<View>(GHOST_BTN_TAG) != null) return

    val ctx: Context = parent.context

    val ghostBtn = ImageButton(ctx).apply {
        tag = GHOST_BTN_TAG
        // Use system fallback icon (same as InstaEclipse's catch branch).
        setImageResource(android.R.drawable.ic_menu_view)
        setColorFilter(Color.WHITE)
        background = null

        val size = dp(ctx, 35)
        layoutParams = FrameLayout.LayoutParams(size, size).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            setMargins(dp(ctx, 5), 25, 0, 0)
        }

        setOnClickListener { triggerDMSeen(parent) }
    }

    // Post to avoid mutating the hierarchy during a layout pass.
    parent.post {
        parent.addView(ghostBtn, 3)
    }
}

private fun triggerDMSeen(view: View) {
    try {
        val ctx: Context = view.context
        @SuppressLint("DiscouragedApi")
        val messageListId = ctx.resources.getIdentifier(
            "message_list", "id", ctx.packageName
        )

        val messageList = view.rootView.findViewById<View>(messageListId)
        if (messageList is ViewGroup) {
            // Large value is capped by RecyclerView to the actual bottom.
            messageList.scrollBy(0, 100_000)

            messageList.scrollBy(0, -200)

            view.postDelayed({
                messageList.scrollBy(0, 200)
                Toast.makeText(
                    ctx,
                    "DM marked as read 👻",
                    Toast.LENGTH_SHORT
                ).show()
            }, 300)
        }
    } catch (e: Exception) {
        Logger.printException({ "GhostDMMarkAsRead trigger failed" }, e)
    }
}

private fun dp(ctx: Context, v: Int): Int =
    (v * ctx.resources.displayMetrics.density).toInt()
