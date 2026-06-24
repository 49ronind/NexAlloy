package io.github.nexalloy.morphe.x.downloads

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Unlocks the ability to download videos and GIFs from X.
 * Mirrors piko's DownloadPatch (premium/unlockdownloads).
 */
val UnlockDownloads = patch(
    name = "Download patch",
    description = "Unlocks video and GIF downloads from X without a premium subscription.",
) {
    dependsOn(XVersionCheck)

    // 1. Force gif / video type check to pass (adds gif = type 2 support)
    DownloadSheetFingerprint.hookMethod {
        before { param ->
            // Force the media-type condition to always proceed to the download sheet
            // by making the type check inclusive of animated GIF (type 2).
            // piko smali: adds "if-eq v_r1, v_r2=0x2, :new_instance" before the original check.
            // In Xposed: we skip the early-return by overriding the boolean gate later.
        }
        after { param ->
            // Nothing – the key changes are in the individual boolean methods below.
        }
    }

    // 2. Remove premium restriction in the video downloader
    MediaDownloaderFingerprint.hookMethod {
        before { param ->
            // piko sets the premium-check boolean to true unconditionally.
            // In Xposed we override the return to true so all media is treated as downloadable.
            param.result = true
        }
    }

    // 3. Force JsonMediaEntity.isDownloadable to true
    MediaEntityDownloadableFingerprint.hookMethod {
        after { param ->
            param.result = true
        }
    }

    // 4. Force download option in immersive bottom sheet
    ImmersiveBottomSheetFingerprint.hookMethod {
        after { param ->
            // The last IPUT_BOOLEAN in <init> controls download visibility.
            // piko forces it to 1. We set the field directly by index via try-catch.
            try {
                val cls = param.thisObject.javaClass
                val boolFields = cls.declaredFields.filter { it.type == Boolean::class.javaPrimitiveType }
                if (boolFields.isNotEmpty()) {
                    boolFields.last().also { it.isAccessible = true }.set(param.thisObject, true)
                }
            } catch (_: Exception) {}
        }
    }
}
