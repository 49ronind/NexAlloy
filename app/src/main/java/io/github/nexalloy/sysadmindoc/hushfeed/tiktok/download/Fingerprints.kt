package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.download

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.methodCall
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

object AwemeGetVideoFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
    name = "getVideo",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
    parameters = emptyList(),
)

@StrictQuery
object CommentImageWatermarkFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/graphics/Bitmap;"),
    strings = listOf("[tiktok_logo]"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/graphics/Canvas;",
            name = "drawBitmap",
            parameters = listOf("Landroid/graphics/Bitmap;", "F", "F", "Landroid/graphics/Paint;"),
            returnType = "V",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
    ),
)

object PostDownloadShareSheetFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/internalshare/impl/fragment/DownloadAndShareFragment;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("after_video_saved_share_to_nscreen"),
)
