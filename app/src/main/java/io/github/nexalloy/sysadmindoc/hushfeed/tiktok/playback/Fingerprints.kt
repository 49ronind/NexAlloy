package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.playback

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodDirect

object VideoEngineSetLoopingFingerprint : Fingerprint(
    definingClass = "Lcom/ss/ttvideoengine/TTVideoEngine;",
    name = "setLooping",
    returnType = "V",
    parameters = listOf("Z"),
)

@StrictQuery
object ShouldShowProgressBarFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/VideoControl;",
            name = "showProgressBar",
            opcode = Opcode.IGET,
        ),
    ),
)

val setSeekBarShowTypeMethod = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("seekbar show type change, change to:")
        }
    }.single { it.paramTypeNames.lastOrNull() == "int" }
}
