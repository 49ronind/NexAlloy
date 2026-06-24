package io.github.nexalloy.morphe.x.sharemenu

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

/** The enum class that holds all share-menu action items. */
internal object ShareMenuActionEnumFingerprint : Fingerprint(
    strings = listOf(
        "SEND_VIA_DM", "COPY_LINK", "SHARE_VIA", "BOOKMARK",
    ),
)

/** Share menu bottom-sheet binding method. */
internal object ShareMenuBindingFingerprint : Fingerprint(
    definingClass = "/sharemenu/",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("shareMenuItems"),
)

/** Download action entry point inside the share menu. */
internal object ShareMenuDownloadFingerprint : Fingerprint(
    definingClass = "/sharemenu/",
    returnType = "V",
    strings = listOf("DOWNLOAD", "download"),
)

/** Reader-mode toggle method. */
internal object ReaderModeToggleFingerprint : Fingerprint(
    strings = listOf("reader_mode", "article_reader_enabled"),
)

/** In-app translation method. */
internal object TranslatorFingerprint : Fingerprint(
    strings = listOf("translation", "TRANSLATE", "translateText"),
)

/** Share-as-image method. */
internal object ShareImageFingerprint : Fingerprint(
    strings = listOf("share_image", "captureScreenshot", "SHARE_IMAGE"),
)

/** Open-in-browser method for browsing tweet object. */
internal object BrowseObjectFingerprint : Fingerprint(
    strings = listOf("tweet_object_url", "browser_open", "browseObject"),
)

/** Inline download button insertion method. */
internal object InlineDownloadFingerprint : Fingerprint(
    definingClass = "/inlineactions/",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("download"),
)
