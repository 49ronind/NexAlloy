package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.feed

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall
import org.luckypray.dexkit.query.enums.StringMatchType

private const val AWEME_DESC = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;"
private const val FEED_ITEM_LIST_DESC = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;"
private const val SPLASH_AD_SERVICE_DESC =
    "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/service/ISplashAdService;"
private const val USER_DESC = "Lcom/ss/android/ugc/aweme/profile/model/User;"
private const val VIDEO_ITEM_PARAMS_DESC = "Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"
private const val PROFILE_AWEME_LIST_FRAGMENT =
    "Lcom/ss/android/ugc/profile/business/profile/tab/AwemeListFragmentImpl;"
private const val TAKO_RIGHT_BOTTOM_ENTRANCE =
    "Lcom/ss/android/ugc/aweme/tako/otherpage/feed/mainentrance/ui/"

object MainFeedResponseFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
    name = "fetchFeedList",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = FEED_ITEM_LIST_DESC,
    custom = {
        paramCount(1)
    },
)

@StrictQuery
object FeedApiFetchFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/api/FeedApi;",
    returnType = FEED_ITEM_LIST_DESC,
    filters = listOf(
        fieldAccess(
            definingClass = FEED_ITEM_LIST_DESC,
            name = "preloadAds",
            type = "Ljava/util/List;",
            opcode = Opcode.IGET_OBJECT,
        ),
        methodCall(
            definingClass = SPLASH_AD_SERVICE_DESC,
            parameters = listOf("Ljava/util/List;"),
        ),
    ),
    custom = {
        paramCount(1)
    },
)

val splashAdPreloadMethods = findMethodListDirect {
    findClass {
        matcher {
            addInterface("com.bytedance.ies.ugc.aweme.commercialize.splash.service.ISplashAdService")
        }
    }.findMethod {
        matcher {
            paramTypes("java.util.List")
        }
    }
}

object FeedItemListGetItemsFingerprint : Fingerprint(
    definingClass = FEED_ITEM_LIST_DESC,
    name = "getItems",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
)

object ProfileApiExecuteFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/services/ProfileDependentComponentImpl;",
    name = "apiExecuteGetJSONObject",
    returnType = "Ljava/lang/Object;",
    custom = {
        paramCount(7)
    },
)

object FollowFeedListGetItemsFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;",
    name = "getItems",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
)

object FriendsFeedAwemeListFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/friendstab/api/FriendsFeedResponse;",
    name = "getAwemeList",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
)

object RepostFeedDataFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/repostfeed/model/RepostFeedResponse;",
    name = "getRepostFeedData",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
)

object ProfileRefreshResultFingerprint : Fingerprint(
    definingClass = PROFILE_AWEME_LIST_FRAGMENT,
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "Z"),
    strings = listOf("onRefreshResult: type="),
)

object ProfileLoadMoreResultFingerprint : Fingerprint(
    definingClass = PROFILE_AWEME_LIST_FRAGMENT,
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "Z"),
    strings = listOf("onLoadMoreResult: type="),
)

object ProfileLoadLatestResultFingerprint : Fingerprint(
    definingClass = PROFILE_AWEME_LIST_FRAGMENT,
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "Z"),
    strings = listOf("onLoadLatestResult: type="),
)

object MidAdReplaceFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/feed/platform/panel/midad/MidAdComponent;",
    isStatic = true,
    returnType = "V",
    strings = listOf("midroll_ads_show"),
    custom = {
        paramCount(5)
    },
)

object ProfileAdEligibilityFingerprint : Fingerprint(
    isStatic = true,
    returnType = "Z",
    parameters = listOf(USER_DESC),
    strings = listOf("profile_ad_experiment"),
)

object SearchResultRequestIdFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/topsearch/core/model/SearchMixFeedList;",
    name = "setRequestId",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

@StrictQuery
object TouchPointPendantParserFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/util/HashMap;", "Ljava/util/List;", "Z"),
    filters = listOf(
        checkCast("Lcom/bytedance/touchpoint/api/model/NormalPendant;"),
    ),
)

@StrictQuery
object ProfileRewardsIconBinderFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        fieldAccess(definingClass = "Lcom/bytedance/touchpoint/api/model/ProfileActivityIcon;"),
        checkCast("Lcom/bytedance/tux/icon/TuxIconView;"),
        methodCall(
            definingClass = "Landroid/view/View;",
            name = "setId",
            parameters = listOf("I"),
            returnType = "V",
        ),
    ),
)

// Tako AI suppression fingerprints (HushFeed 47.9.3)

object TakoAiFeedButtonSetVisibleFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z"),
    strings = listOf("right_container_chat_gpt"),
) {
    init {
        classMatcher { className("TakoAssem", StringMatchType.EndsWith) }
    }
}

object TakoTopIconCreateFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/tako/feed/topicon/TakoTopIconAssem;",
    name = "onCreate",
    returnType = "V",
    parameters = emptyList(),
)

@StrictQuery
object TakoAskBarBindFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/tako/detail/keyframe/ui/TakoDetailKeyFrameBottomAssemAssem;",
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(checkCast(VIDEO_ITEM_PARAMS_DESC)),
)

object TakoAskBarTriggerBindFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/tako/detail/keyframe/ui/TakoDetailKeyFrameBottomTrigger;",
    returnType = "V",
    parameters = listOf(VIDEO_ITEM_PARAMS_DESC),
)

object TakoFeedTriggerBindFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/tikbot/TakoTrigger;",
    returnType = "V",
    parameters = listOf(VIDEO_ITEM_PARAMS_DESC),
)

object TakoFeedTriggerRoofBindFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/tikbot/TakoTriggerRoof;",
    returnType = "V",
    parameters = listOf(VIDEO_ITEM_PARAMS_DESC),
)

object TakoRightBottomFixedEntranceBindFingerprint : Fingerprint(
    definingClass = "${TAKO_RIGHT_BOTTOM_ENTRANCE}TakoRightBottomFixedEntrance;",
    returnType = "V",
    parameters = listOf(VIDEO_ITEM_PARAMS_DESC),
)

object TakoRightBottomInCellEntranceBindFingerprint : Fingerprint(
    definingClass = "${TAKO_RIGHT_BOTTOM_ENTRANCE}TakoRightBottomInCellEntrance;",
    returnType = "V",
    parameters = listOf(VIDEO_ITEM_PARAMS_DESC),
)

object TakoSearchBubbleInflateFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/search/pages/middlepage/takoentrance/ui/TakoSingleRoundButtonAssem;",
    isStatic = false,
    returnType = "Landroid/view/View;",
    parameters = listOf("Landroid/view/ViewStub;"),
)

object TakoSearchPillInflateFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/search/pages/middlepage/takoentrance/ui/MixedRoundFloatingButtonAssem;",
    isStatic = false,
    returnType = "Landroid/view/View;",
    parameters = listOf("Landroid/view/ViewStub;"),
)

object TakoCommentTopBarCanShowFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/tako/detail/related/TakoCommentTopBarServiceImpl;",
    name = "canShow",
    returnType = "Z",
    custom = {
        paramCount(5)
    },
)
