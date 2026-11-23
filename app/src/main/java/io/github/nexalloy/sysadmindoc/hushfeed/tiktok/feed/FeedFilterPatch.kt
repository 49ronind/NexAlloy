package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.feed

import android.view.View
import io.github.nexalloy.callMethodOrNull
import io.github.nexalloy.getIntFieldOrNull
import io.github.nexalloy.getObjectFieldOrNull
import io.github.nexalloy.patch
import io.github.nexalloy.setObjectField
import org.json.JSONObject
import java.util.WeakHashMap

internal object FeedFilterConfig {
    @Volatile var removeAds: Boolean = false
    @Volatile var hideLive: Boolean = false
    @Volatile var hideShop: Boolean = false
    @Volatile var hideStories: Boolean = false
}

internal object FeedFilterEngine {
    private const val AWEME_TYPE_STORY = 40
    private const val AWEME_TYPE_STORY_SHARED = 45
    private const val AWEME_TYPE_LIVE = 101
    private const val AWEME_TYPE_INSERT_CARD = 105
    private const val LEMON8_CARD_TYPE = 9
    private const val LYNX_CARD_IMPL_TYPE = 2
    private val LEMON8_LYNX_CARD_TYPES = setOf(40, 105, 114)
    private const val SHOP_PLACEHOLDER = "placeholder_product_id"
    private const val PANEL_DISCLOSURE = "panel_top_disclosure_label"
    private val ROOM_GOODS_FLAGS = arrayOf(
        "hasCommerceGoods",
        "existedCommerceGoods",
        "hasTtlsGoods",
        "existedTtlsGoods",
    )
    private val SEARCH_AD_FIELDS = arrayOf("multiAdCard", "aiAdCard", "brandZoneCard")

    private val listLocks = Array(16) { Any() }
    private val parsedProfileLists = WeakHashMap<Any, Boolean>()

    private fun lockFor(owner: Any): Any =
        listLocks[(System.identityHashCode(owner) and 0x7FFFFFFF) % listLocks.size]

    fun markProfileList(obj: Any?) {
        if (obj == null) return
        synchronized(parsedProfileLists) {
            parsedProfileLists[obj] = true
        }
    }

    private fun isProfileList(feedItemList: Any): Boolean {
        val uid = feedItemList.getObjectFieldOrNull("dataUserId") as? String
        if (!uid.isNullOrEmpty()) return true
        synchronized(parsedProfileLists) {
            return parsedProfileLists.containsKey(feedItemList)
        }
    }

    private fun prop(obj: Any?, getter: String, field: String): Any? {
        if (obj == null) return null
        return obj.callMethodOrNull(getter) ?: obj.getObjectFieldOrNull(field)
    }

    private fun nonZero(obj: Any, getter: String, field: String): Boolean {
        val v = prop(obj, getter, field) as? Number ?: return false
        return v.toInt() != 0
    }

    private fun nonBlank(value: Any?): Boolean =
        value is String && value.trim().isNotEmpty()

    private fun awemeType(aweme: Any): Int =
        (aweme.callMethodOrNull("getAwemeType") as? Number)?.toInt()
            ?: aweme.getIntFieldOrNull("awemeType")
            ?: 0

    // --- Ad Detection (HushFeed AdsFilter + ContentMarkerFilters) ---

    fun isAd(aweme: Any): Boolean {
        if (aweme.callMethodOrNull("isAd") == true) return true
        if (aweme.callMethodOrNull("isSoftAd") == true) return true
        if (aweme.callMethodOrNull("getAwemeRawAd") != null) return true
        if (aweme.callMethodOrNull("isWithPromotionalMusic") == true) return true
        if (isPseudoAd(aweme)) return true
        if (isLemon8PromoCard(aweme)) return true
        if (hasCreatorCommissionDisclosure(aweme)) return true
        if (hasPaidPartnershipMarker(aweme)) return true
        return false
    }

    private fun isPseudoAd(aweme: Any): Boolean {
        val commerce = prop(aweme, "getCommerceVideoAuthInfo", "commerceVideoAuthInfo") ?: return false
        return prop(commerce, "isPseudoAd", "isPseudoAd") == true &&
            prop(commerce, "getPseudoAdData", "pseudoAdData") != null
    }

    private fun isLemon8PromoCard(aweme: Any): Boolean {
        val insert = prop(aweme, "getCardInsertInfo", "cardInsertInfo") ?: return false
        val cardType = (prop(insert, "getCardType", "cardType") as? Number)?.toInt() ?: return false
        if (cardType == LEMON8_CARD_TYPE) return true
        if (cardType !in LEMON8_LYNX_CARD_TYPES || awemeType(aweme) != AWEME_TYPE_INSERT_CARD) return false
        val implType = (prop(insert, "getCardImplType", "cardImplType") as? Number)?.toInt()
        return implType == LYNX_CARD_IMPL_TYPE
    }

    private fun hasCreatorCommissionDisclosure(aweme: Any): Boolean {
        val content = aweme.callMethodOrNull("getContentModel")
        val business = content?.getObjectFieldOrNull("standardBusinessModel")
        val alliance = business?.callMethodOrNull("getLocalAllianceInfo")
        if (alliance?.callMethodOrNull("showBottomLabel") == true) return true

        val extras = (aweme.callMethodOrNull("getAnchorsExtras") as? String)?.trim()
        if (extras.isNullOrEmpty() || !extras.contains("\"$PANEL_DISCLOSURE\"")) return false
        return runCatching {
            val disclosure = JSONObject(extras).optJSONObject(PANEL_DISCLOSURE) ?: return@runCatching false
            nonBlank(disclosure.opt("display_text")) || nonBlank(disclosure.opt("truncatable_text"))
        }.getOrDefault(false)
    }

    private fun hasPaidPartnershipMarker(aweme: Any): Boolean {
        val accounts = prop(aweme, "getBrandContentAccounts", "brandContentAccounts") as? Collection<*>
        if (!accounts.isNullOrEmpty()) return true

        val commerce = prop(aweme, "getCommerceVideoAuthInfo", "commerceVideoAuthInfo")
        if (commerce != null) {
            if (prop(commerce, "isBrandedContent", "isBrandedContent") == true ||
                prop(commerce, "isBrandOrganicContent", "isBrandOrganicContent") == true ||
                nonZero(commerce, "getBrandedContentType", "brandedContentType") ||
                nonZero(commerce, "getBrandOrganicType", "brandOrganicType") ||
                nonBlank(prop(commerce, "getEcSearchBoBcLabelText", "ecSearchBoBcLabelText")) ||
                prop(commerce, "isCommerce", "isCommerce") == true
            ) {
                return true
            }
        }
        return nonBlank(prop(aweme, "getCommercialVideoInfo", "commercialVideoInfo"))
    }

    // --- LIVE / Shop / Story Detection (HushFeed) ---

    private fun liveRooms(aweme: Any): List<Any> {
        val rooms = ArrayList<Any>(4)
        fun addRoom(r: Any?) {
            if (r != null && !rooms.contains(r)) rooms.add(r)
        }
        addRoom(aweme.getObjectFieldOrNull("newLiveRoomData"))
        addRoom(aweme.getObjectFieldOrNull("cachedLiveRoomStruct"))
        addRoom(aweme.getObjectFieldOrNull("room"))
        val cell = aweme.callMethodOrNull("getRoomFeedCellStruct")
            ?: aweme.getObjectFieldOrNull("mRoomFeedCellStruct")
        if (cell != null) {
            addRoom(cell.callMethodOrNull("getNewLiveRoomData"))
            addRoom(cell.getObjectFieldOrNull("room"))
            addRoom(cell.getObjectFieldOrNull("newLiveRoomData"))
        }
        return rooms
    }

    private fun isValidRoom(room: Any?, idField: String): Boolean {
        if (room == null) return false
        val id = (room.getObjectFieldOrNull(idField) as? Number)?.toLong() ?: 0L
        if (id <= 0L) return false
        if (idField != "roomId" && room.getObjectFieldOrNull("owner") == null) return false
        return true
    }

    fun isLive(aweme: Any): Boolean {
        val liveId = (aweme.callMethodOrNull("getLiveId") as? Number)?.toLong() ?: 0L
        if (liveId > 0L) return true
        if (aweme.callMethodOrNull("isLiveReplay") == true) return true
        val liveType = aweme.callMethodOrNull("getLiveType") as? String
        if (!liveType.isNullOrEmpty()) return true
        if (awemeType(aweme) == AWEME_TYPE_LIVE) return true
        if (isValidRoom(aweme.getObjectFieldOrNull("newLiveRoomData"), "id")) return true
        if (isValidRoom(aweme.getObjectFieldOrNull("cachedLiveRoomStruct"), "id")) return true
        if (isValidRoom(aweme.getObjectFieldOrNull("room"), "roomId")) return true
        val cell = aweme.callMethodOrNull("getRoomFeedCellStruct")
            ?: aweme.getObjectFieldOrNull("mRoomFeedCellStruct")
        if (cell != null) {
            if (isValidRoom(cell.callMethodOrNull("getNewLiveRoomData"), "id") ||
                isValidRoom(cell.getObjectFieldOrNull("room"), "id") ||
                isValidRoom(cell.getObjectFieldOrNull("newLiveRoomData"), "id")
            ) return true
        }
        return false
    }

    fun isShop(aweme: Any): Boolean {
        val shareUrl = aweme.callMethodOrNull("getShareUrl") as? String
        if (shareUrl != null && shareUrl.contains(SHOP_PLACEHOLDER)) return true
        if (!isLive(aweme)) return false
        if (aweme.getObjectFieldOrNull("isLiveHasProduct") == true) return true
        for (room in liveRooms(aweme)) {
            for (flag in ROOM_GOODS_FLAGS) {
                if (room.getObjectFieldOrNull(flag) == true) return true
            }
            val num = room.getObjectFieldOrNull("fypCommerceStruct")
                ?.getObjectFieldOrNull("productNum") as? Number
            if (num != null && num.toLong() > 0L) return true
        }
        return false
    }

    fun isStory(aweme: Any): Boolean {
        if (aweme.callMethodOrNull("getIsTikTokStory") == true) return true
        val t = awemeType(aweme)
        return t == AWEME_TYPE_STORY || t == AWEME_TYPE_STORY_SHARED
    }

    fun shouldDrop(aweme: Any?, adsOnly: Boolean = false): Boolean {
        if (aweme == null) return false
        if (FeedFilterConfig.removeAds && isAd(aweme)) return true
        if (adsOnly) return false
        if (FeedFilterConfig.hideLive && isLive(aweme)) return true
        if (FeedFilterConfig.hideShop && isShop(aweme)) return true
        if (FeedFilterConfig.hideStories && isStory(aweme)) return true
        return false
    }

    private fun extractAweme(item: Any?): Any? {
        if (item == null) return null
        if (item.javaClass.name == "com.ss.android.ugc.aweme.feed.model.Aweme") return item
        return item.callMethodOrNull("getAweme")
            ?: item.getObjectFieldOrNull("aweme")
    }

    @Suppress("UNCHECKED_CAST")
    fun filterListInPlace(owner: Any, list: List<*>?, adsOnly: Boolean = false, replaceOnImmutable: ((List<Any?>) -> Unit)? = null) {
        if (list.isNullOrEmpty()) return
        synchronized(lockFor(owner)) {
            val kept = ArrayList<Any?>(list.size)
            var removed = 0
            for (entry in list) {
                val aweme = extractAweme(entry)
                if (aweme != null && shouldDrop(aweme, adsOnly)) {
                    removed++
                } else {
                    kept.add(entry)
                }
            }
            if (removed == 0) return
            // Guard: never empty an entire profile or feed batch if every item matched unexpectedly.
            if (kept.isEmpty() && list.size > 1 && !adsOnly) return
            val mutable = list as? MutableList<Any?>
            if (mutable != null) {
                runCatching {
                    mutable.clear()
                    mutable.addAll(kept)
                }.onFailure {
                    replaceOnImmutable?.invoke(kept)
                }
            } else {
                replaceOnImmutable?.invoke(kept)
            }
        }
    }

    fun filterFeedItemList(feedItemList: Any?) {
        if (feedItemList == null) return
        val adsOnly = isProfileList(feedItemList)
        val items = feedItemList.getObjectFieldOrNull("items") as? List<*> ?: return
        filterListInPlace(feedItemList, items, adsOnly) { kept ->
            runCatching { feedItemList.setObjectField("items", kept) }
        }
    }

    fun clearPreloadAds(feedItemList: Any?) {
        if (feedItemList == null || !FeedFilterConfig.removeAds) return
        runCatching {
            val preload = feedItemList.getObjectFieldOrNull("preloadAds") as? List<*>
            if (!preload.isNullOrEmpty()) {
                feedItemList.setObjectField("preloadAds", emptyList<Any>())
            }
        }
    }

    private fun isSearchAd(card: Any?): Boolean {
        if (card == null) return false
        if (card.callMethodOrNull("isAdOrContainAd") == true) return true
        for (field in SEARCH_AD_FIELDS) {
            if (card.getObjectFieldOrNull(field) != null) return true
        }
        val preciseAd = card.getObjectFieldOrNull("preciseAd")
        if (preciseAd != null) {
            val cards = preciseAd.getObjectFieldOrNull("awemeCards") as? List<*>
            if (!cards.isNullOrEmpty()) return true
        }
        val aweme = card.getObjectFieldOrNull("aweme")
        return aweme != null && isAd(aweme)
    }

    fun filterSearchMixFeedList(searchResult: Any?) {
        if (searchResult == null || !FeedFilterConfig.removeAds) return
        val items = searchResult.getObjectFieldOrNull("mItems") as? List<*> ?: return
        if (items.isEmpty()) return
        val kept = ArrayList<Any?>(items.size)
        for (card in items) {
            if (!isSearchAd(card)) kept.add(card)
        }
        // HushFeed guard: never wipe an entire search page.
        if (kept.size == items.size || kept.isEmpty()) return
        runCatching { searchResult.setObjectField("mItems", kept) }
    }
}

private val FeedFilterCore = patch(
    name = "Feed filter core",
    description = "Shared multi-surface feed filter hooks (HushFeed 47.9.3).",
) {
    // Track profile responses so profile grids only strip ads, not creator posts.
    runCatching {
        ProfileApiExecuteFingerprint.hookMethod {
            after { param ->
                val result = param.result ?: return@after
                if (result.javaClass.name == "com.ss.android.ugc.aweme.feed.model.FeedItemList") {
                    FeedFilterEngine.markProfileList(result)
                }
            }
        }
    }

    // For You response + cold-start TopView splash preload ads.
    runCatching {
        MainFeedResponseFingerprint.hookMethod {
            after { param ->
                val feedList = param.result ?: return@after
                FeedFilterEngine.clearPreloadAds(feedList)
                FeedFilterEngine.filterFeedItemList(feedList)
            }
        }
    }

    // Intercept TopView splash preload handoff before ISplashAdService schedules preload tasks.
    runCatching {
        ::splashAdPreloadMethods.dexMethodList.forEach { method ->
            method.hookMethod {
                before { param ->
                    if (FeedFilterConfig.removeAds) {
                        param.args[0] = emptyList<Any>()
                    }
                }
            }
        }
    }

    runCatching {
        FeedApiFetchFingerprint.hookMethod {
            after { param ->
                val feedList = param.result ?: return@after
                FeedFilterEngine.clearPreloadAds(feedList)
            }
        }
    }

    // Late safety net on FeedItemList.getItems() (catches cached & restored 47.x lists).
    runCatching {
        FeedItemListGetItemsFingerprint.hookMethod {
            before { param ->
                FeedFilterEngine.filterFeedItemList(param.thisObject)
            }
        }
    }

    // Following tab.
    runCatching {
        FollowFeedListGetItemsFingerprint.hookMethod {
            after { param ->
                val owner = param.thisObject ?: return@after
                val list = param.result as? List<*> ?: return@after
                FeedFilterEngine.filterListInPlace(owner, list) { kept ->
                    param.result = kept
                }
            }
        }
    }

    // Friends tab.
    runCatching {
        FriendsFeedAwemeListFingerprint.hookMethod {
            after { param ->
                val owner = param.thisObject ?: return@after
                val list = param.result as? List<*> ?: return@after
                FeedFilterEngine.filterListInPlace(owner, list) { kept ->
                    param.result = kept
                }
            }
        }
    }

    // Reposts feed.
    runCatching {
        RepostFeedDataFingerprint.hookMethod {
            after { param ->
                val owner = param.thisObject ?: return@after
                val list = param.result as? List<*> ?: return@after
                FeedFilterEngine.filterListInPlace(owner, list) { kept ->
                    param.result = kept
                }
            }
        }
    }

    // Profile list callbacks (ads only).
    listOf(
        ProfileRefreshResultFingerprint,
        ProfileLoadMoreResultFingerprint,
        ProfileLoadLatestResultFingerprint,
    ).forEach { fp ->
        runCatching {
            fp.hookMethod {
                before { param ->
                    val list = param.args.getOrNull(0) as? List<*> ?: return@before
                    FeedFilterEngine.filterListInPlace(param.thisObject ?: list, list, adsOnly = true) { kept ->
                        param.args[0] = kept
                    }
                }
            }
        }
    }

    // Mid-roll ad splice into pager adapter.
    runCatching {
        MidAdReplaceFingerprint.hookMethod {
            before { param ->
                if (FeedFilterConfig.removeAds) {
                    param.result = null
                }
            }
        }
    }

    // Profile pager ad eligibility gate.
    runCatching {
        ProfileAdEligibilityFingerprint.hookMethod {
            before { param ->
                if (FeedFilterConfig.removeAds) {
                    param.result = false
                }
            }
        }
    }

    // Search results ads.
    runCatching {
        SearchResultRequestIdFingerprint.hookMethod {
            before { param ->
                FeedFilterEngine.filterSearchMixFeedList(param.thisObject)
            }
        }
    }
}

val FeedAdFilter = patch(
    name = "Feed ad removal",
    description = "Removes ads across For You, TopView splash, Following, Friends, Reposts, Profiles, mid-roll splices, Search, and floating promo badges (HushFeed).",
) {
    FeedFilterConfig.removeAds = true
    dependsOn(FeedFilterCore)

    // Hide floating promotion pendants (coins/timers/badges).
    runCatching {
        TouchPointPendantParserFingerprint.hookMethod {
            before { param ->
                val pendantList = param.args.getOrNull(1) as? MutableList<*>
                pendantList?.clear()
                param.result = null
            }
        }
    }

    // Hide purple profile rewards shortcut.
    runCatching {
        ProfileRewardsIconBinderFingerprint.hookMethod {
            after { param ->
                (param.result as? View)?.visibility = View.GONE
            }
        }
    }
}

val HideLiveVideos = patch(
    name = "Hide LIVE videos",
    description = "Filters LIVE streams out of feed lists (HushFeed).",
    use = false,
) {
    FeedFilterConfig.hideLive = true
    dependsOn(FeedFilterCore)
}

val HideShopVideos = patch(
    name = "Hide Shop posts",
    description = "Filters TikTok Shop product cards and shopping LIVEs out of feed lists (HushFeed).",
    use = false,
) {
    FeedFilterConfig.hideShop = true
    dependsOn(FeedFilterCore)
}

val HideStories = patch(
    name = "Hide stories",
    description = "Filters TikTok Stories out of feed lists (HushFeed).",
    use = false,
) {
    FeedFilterConfig.hideStories = true
    dependsOn(FeedFilterCore)
}

val HideTakoAi = patch(
    name = "Hide Tako AI buttons",
    description = "Hides Tako AI floating buttons, top bar icon, caption Ask strips, search entrances, and comment bars (HushFeed).",
    use = false,
) {
    runCatching {
        TakoAiFeedButtonSetVisibleFingerprint.hookMethod {
            before { param ->
                param.args[0] = false
            }
        }
    }
    runCatching {
        TakoTopIconCreateFingerprint.hookMethod {
            before { param ->
                // Let parent lifecycle run via getContentView hiding; skip registering A/B icon callback safely.
                runCatching {
                    (param.thisObject?.callMethodOrNull("getContentView") as? View)?.visibility = View.GONE
                }
            }
        }
    }
    runCatching {
        TakoAskBarBindFingerprint.hookMethod {
            before { param ->
                (param.thisObject?.callMethodOrNull("getContentView") as? View)?.visibility = View.GONE
                param.result = null
            }
        }
    }
    listOf(
        TakoAskBarTriggerBindFingerprint,
        TakoFeedTriggerBindFingerprint,
        TakoFeedTriggerRoofBindFingerprint,
        TakoRightBottomFixedEntranceBindFingerprint,
        TakoRightBottomInCellEntranceBindFingerprint,
    ).forEach { fp ->
        runCatching {
            fp.hookMethod {
                before { param ->
                    param.result = null
                }
            }
        }
    }
    listOf(
        TakoSearchBubbleInflateFingerprint,
        TakoSearchPillInflateFingerprint,
    ).forEach { fp ->
        runCatching {
            fp.hookMethod {
                before { param ->
                    param.result = null
                }
            }
        }
    }
    runCatching {
        TakoCommentTopBarCanShowFingerprint.hookMethod {
            before { param ->
                param.result = false
            }
        }
    }
}
