package io.github.nexalloy.revanced.facebook.ad

import io.github.nexalloy.patch
import io.github.nexalloy.revanced.facebook.hookAdQueryFetch
import io.github.nexalloy.revanced.facebook.hookAdRequestNoOp
import io.github.nexalloy.revanced.facebook.hookEmptyCollectionResult
import io.github.nexalloy.revanced.facebook.hookForceBoolean

/**
 * Stops advertisements being requested, rather than removing them once they arrive.
 *
 * The rest of this module works downstream: it watches lists of stories go past and takes
 * out the ones it can prove are sponsored. That works, but it inherits a hard problem —
 * every filter has to recognise an ad, and every regression this module has had came from
 * a recognition test answering "yes" to something organic. This patch avoids the question
 * entirely. A request that is never made returns nothing to recognise.
 *
 * It is a separate toggle from [HideFacebookAds] because it acts on different code at a
 * different moment, and because its failure mode is different too: where a bad filter
 * blanks a surface, a bad request hook leaves a surface waiting for data that never
 * comes. Turning this off restores every fetch while the filters keep running.
 *
 * Nine pipelines, each with no coverage before:
 *
 *  - **The news feed's async ad channel.** The big one. Ads no longer ride along with the
 *    feed response; they are fetched separately and spliced in as they land, which is
 *    precisely why the CSR filter and the sponsored pool — both of which only ever see
 *    the feed response — never saw them.
 *  - **The Stories viewer's ad pagination.** The newer of the two paginating sources,
 *    the one that does not log "ads_deletion" and so was missed by the provider hooks.
 *  - **The Reels video-ad fetch.** Tops up the ad supply for the main Reels viewer.
 *  - **Real-time-intent insertion in Video Home.** Placement rather than fetch: the ad
 *    exists, this is the step that puts it in the list you scroll.
 *  - **The position-one feed ad.** The advert in the first slot of the news feed, which
 *    gets its slot from a session budget rather than from the feed ranker.
 *  - **The ad-channel network layer.** One level below the async-ad controller. The
 *    controller hooks stop it *deciding* to request; these stop the request reaching the
 *    wire, which matters because the feed, Reels and the mid-session sponsored-story
 *    top-up each get there through callers the controller does not own.
 *  - **The rest of the Video Home / Reels pipeline.** The fetch, the general insertion
 *    step and the delayed real-time-intent render, plus the mid-card ad survey. The
 *    existing hook covers one insertion point for one ad kind; this covers the routes
 *    every other Reels ad takes.
 *  - **The Stories viewer's payload fetch.** A second method, separate from the one
 *    already blocked, through which the viewer was still topping up its ad buckets.
 *  - **The search "AI mode" ad story query.** Its sibling query — the one that decides
 *    *which* ads to show — was already blocked; this is the one that fetches the story
 *    behind an ad that has already been chosen, so ads could still hydrate from cache.
 *
 * Every hook is shape-checked before it is installed. A `void` method is skipped, a
 * method returning a list is given an empty one, a boolean gate is answered false; a
 * method whose return type does not match what the hook can produce is left alone rather
 * than handed a null it would crash on.
 *
 * Chín pipeline đó — cùng bảy pipeline nữa thêm vào sau — được phân giải trong MỘT
 * fingerprint duy nhất, [blockAdRequestTargetsFingerprint]. Chi tiết từng mục, và lý do
 * ranh giới cache phải là một chứ không phải mười bảy, nằm ở chú thích của fingerprint đó
 * trong `Fingerprints.kt`.
 */
val BlockFacebookAdRequests = patch(
    name = "Block Facebook ad requests",
    description = "Stops the feed, Stories, Reels and Watch asking for ads in the first place, instead of removing them afterwards. Turn off if a feed or the Stories viewer stops loading.",
) {

    ::blockAdRequestTargetsFingerprint.dexMethodList.forEach { dm ->
        runCatching {
            val method = dm.toMethod()
            val returnType = method.returnType
            when {
                returnType == Void.TYPE ->
                    hookAdRequestNoOp(method)

                returnType == java.lang.Boolean.TYPE || returnType == java.lang.Boolean::class.java ->
                    hookForceBoolean(method, false)

                Iterable::class.java.isAssignableFrom(returnType) ->
                    hookEmptyCollectionResult(method)

                !returnType.isPrimitive ->
                    hookAdQueryFetch(method)
            }
        }
    }
}
