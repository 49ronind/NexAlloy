package io.github.nexalloy.icysymmetra.morphe.tiktok.ad

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint

object MainFeedResponseFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
    name = "fetchFeedList",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
)

object AwemeIsAdFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
    name = "isAd",
    returnType = "Z",
    parameters = emptyList(),
)

object TouchPointPendantParserFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/util/HashMap;", "Ljava/util/List;", "Z"),
    strings = listOf("Lcom/bytedance/touchpoint/api/model/NormalPendant;"),
)
