package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.misc

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.methodCall

object ShareUrlTrackerFingerprint : Fingerprint(
    isStatic = true,
    returnType = "Ljava/lang/String;",
    strings = listOf("utm_campaign", "share_link_id"),
    custom = {
        paramCount(min = 2)
    },
)

object CheckSimChangeTaskFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/legoImpl/task/CheckSIMChangeTask;",
    name = "run",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("last_sim_info", "last_check_time", "ttac_sim_change_detect"),
)

@StrictQuery
object CommentClipDataBuilderFingerprint : Fingerprint(
    isStatic = true,
    returnType = "Landroid/content/ClipData;",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/util/List;"),
    strings = listOf("copy_label"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/ClipData;",
            name = "newPlainText",
        ),
    ),
)

object SparkThirdRouterOpenFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Lcom/bytedance/hybrid/spark/third/router/SparkThirdContext;",
    ),
    strings = listOf("ContainerId", "Context_startActivity_1"),
)

object StoryLinkSheetFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Lcom/ss/android/ugc/aweme/sticker/data/InteractStickerStruct;"),
    strings = listOf("external_website_security_pop_up_window_show"),
)

object SparkActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/bytedance/hybrid/spark/page/SparkActivity;",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    strings = listOf("SparkContextContainerId"),
)
