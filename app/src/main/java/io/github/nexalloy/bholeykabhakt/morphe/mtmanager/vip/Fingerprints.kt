package io.github.nexalloy.bholeykabhakt.morphe.mtmanager.vip

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.methodCall

internal const val VIP_NATIVE_CLASS = "l.\u06e7\u073a\u06e2"
internal const val VIP_BOOL_A = "\u06ec"
internal const val IS_VIP = "\u0730"
internal const val VIP_EXPIRY = "\u0736"

internal const val DIALOG_A_CLASS = "l.\u06e8\u06e1"
internal const val DIALOG_B_CLASS = "l.\u073d\u06ec\u06eb"
internal const val MAIN_ACTIVITY_CLASS = "bin.mt.plus.Main"

internal const val SUBTITLE_CLASS = "l.\u06e4\u1a77\u06e2"
internal const val SUBTITLE_TEXT = "\u05a8"
internal const val SUBTITLE_RES = "\u05ab"

/**
 * Network-storage protocol chooser (`l.\u06e2\u06e4\u1a75`):
 * its SubMenu builder gates the premium protocols on a single static `isVip()` call.
 */
internal object NetStorageChooserFingerprint : Fingerprint(
    definingClass = "Ll/\u06e2\u06e4\u1a75;",
    name = "\u05ab",
    returnType = "V",
    parameters = listOf("Landroid/view/SubMenu;"),
    filters = listOf(methodCall(name = IS_VIP, opcode = Opcode.INVOKE_STATIC)),
)
