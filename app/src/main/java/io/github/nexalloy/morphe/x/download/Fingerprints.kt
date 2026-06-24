package io.github.nexalloy.morphe.x.downloads

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

/** DownloadManager request builder – used to intercept the download path. */
internal object DownloadPathFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("parse(...)"),
        opcode(Opcode.MOVE_RESULT_OBJECT),
    ),
    strings = listOf("guessFileName(...)", "setNotificationVisibility(...)"),
)

/**
 * The downloader call site in the media option sheet.
 * Fingerprinted by two unique strings from piko.
 */
internal object MediaDownloaderFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("url", "video_download"),
    custom = { _, classDef ->
        classDef.contains("tweetview/core/ui/mediaoptionssheet")
    },
)

/** Download callback (copy/handle media link). */
internal object DownloadCallFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/downloader/",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(opcode(Opcode.GOTO)),
    strings = listOf("getString(...)", "isUseSnackbar"),
)

/** MediaEntity isDownloadable flag. */
internal object MediaEntityDownloadableFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/core/JsonMediaEntity;",
    filters = listOf(opcode(Opcode.IGET_BOOLEAN)),
)

/** ImmersiveBottomSheet constructor (for forcing download option). */
internal object ImmersiveBottomSheetFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("captionsState"),
)

/** Media download sheet entry point. */
internal object DownloadSheetFingerprint : Fingerprint(
    strings = listOf("mediaEntity", "media_options_sheet"),
)

/** File downloader permission check. */
internal object FileDownloaderFingerprint : Fingerprint(
    returnType = "Z",
    filters = listOf(opcode(Opcode.IF_EQZ)),
    strings = listOf("mediaEntity", "url"),
)
