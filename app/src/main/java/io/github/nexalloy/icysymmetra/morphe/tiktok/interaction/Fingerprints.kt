package io.github.nexalloy.icysymmetra.morphe.tiktok.interaction

import io.github.nexalloy.morphe.Fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

object AntiRecordingAddedFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I"),
    strings = listOf("[onDisplayAdded]"),
) {
    init {
        classMatcher { className("ClearModePanelComponent", StringMatchType.EndsWith) }
    }
}

object AntiRecordingRemovedFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I"),
    strings = listOf("[onDisplayRemoved]"),
) {
    init {
        classMatcher { className("ClearModePanelComponent", StringMatchType.EndsWith) }
    }
}

object VideoEngineSetLoopingFingerprint : Fingerprint(
    definingClass = "Lcom/ss/ttvideoengine/TTVideoEngine;",
    name = "setLooping",
    returnType = "V",
    parameters = listOf("Z"),
)

object LongPressRepostGateFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/digg/VideoDiggAssem;",
    returnType = "Z",
    parameters = listOf("Landroid/view/View;"),
    strings = listOf(
        "Long press detected on digg button for aweme: ",
        "long_press_like_panel",
    ),
)

object SetSeekBarShowTypeFingerprint : Fingerprint(
    strings = listOf("seekbar show type change, change to:"),
)
