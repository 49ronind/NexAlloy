package io.github.nexalloy.icysymmetra.morphe.tiktok.download

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

object AclCommonShareGetCodeFingerprint : Fingerprint(
    name = "getCode",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "I",
    parameters = emptyList(),
) {
    init {
        classMatcher { className("ACLCommonShare", StringMatchType.EndsWith) }
    }
}

object AclCommonShareGetShowTypeFingerprint : Fingerprint(
    name = "getShowType",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "I",
    parameters = emptyList(),
) {
    init {
        classMatcher { className("ACLCommonShare", StringMatchType.EndsWith) }
    }
}

object AclCommonShareGetTranscodeFingerprint : Fingerprint(
    name = "getTranscode",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "I",
    parameters = emptyList(),
) {
    init {
        classMatcher { className("ACLCommonShare", StringMatchType.EndsWith) }
    }
}
