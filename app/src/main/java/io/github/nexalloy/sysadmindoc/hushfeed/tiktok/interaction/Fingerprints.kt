package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.interaction

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.opcodes
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

object LongPressRepostGateFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/digg/VideoDiggAssem;",
    returnType = "Z",
    parameters = listOf("Landroid/view/View;"),
    strings = listOf(
        "Long press detected on digg button for aweme: ",
        "long_press_like_panel",
    ),
)

object LongPressQuickShareGateCallerFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    custom = {
        opcodes(
            Opcode.INVOKE_STATIC, Opcode.MOVE_RESULT, Opcode.CONST_4, Opcode.CONST_4, Opcode.IF_EQ,
            Opcode.INVOKE_STATIC, Opcode.MOVE_RESULT, Opcode.CONST_4, Opcode.IF_NE,
            Opcode.INVOKE_STATIC, Opcode.MOVE_RESULT, Opcode.IF_NEZ, Opcode.CONST_4, Opcode.RETURN,
        )
    },
)

object CircleSearchBlockFingerprint : Fingerprint(
    name = "invoke",
    returnType = "Ljava/lang/Object;",
    parameters = emptyList(),
    strings = listOf("circle_search_block"),
)
