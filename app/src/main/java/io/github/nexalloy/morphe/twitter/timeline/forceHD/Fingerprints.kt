package io.github.nexalloy.morphe.twitter.timeline.forceHD

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findFieldDirect

internal object PlayerSupportFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    custom = { paramCount = 2 }
) {
    init {
        classMatcher { className("av.player.support", org.luckypray.dexkit.query.enums.StringMatchType.Contains) }
    }
}

internal val playerSupportVideoListFieldResolved = findFieldDirect {
    val instructions = PlayerSupportFingerprint().instructions ?: emptyList()
    instructions.first { it.opcode == Opcode.IGET_OBJECT.opCode }.fieldRef
        ?: throw Exception("playerSupportVideoListField not found")
}
