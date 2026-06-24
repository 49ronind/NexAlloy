package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

// ── TweetInfo hook ───────────────────────────────────────────────────────────

/** JsonApiTweet$$JsonObjectMapper.parse() */
internal object TweetInfoHookFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/api/model/json/core/JsonApiTweet\$\$JsonObjectMapper;",
    name = "parse",
    returnType = "Ljava/lang/Object;",
)

// ── Timeline items ───────────────────────────────────────────────────────────

/** BaseNewTweetsBannerPresenter – controls new-posts banner visibility. */
internal object HideBannerFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/timeline/newtweetsbanner/BaseNewTweetsBannerPresenter;",
    returnType = "Z",
    filters = listOf(opcode(Opcode.RETURN)),
)

/** JsonTimelineTweet – used to suppress hidden replies. */
internal object HideHiddenRepliesFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/timeline/urt/JsonTimelineTweet;",
    returnType = "Ljava/lang/Object;",
)

/** JsonFleetsTimelineResponse – Live Threads. */
internal object HideLiveThreadsFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/fleets/api/json/JsonFleetsTimelineResponse;",
    filters = listOf(opcode(Opcode.IGET_OBJECT)),
)

/** BadgeableTabView.setBadgeNumber. */
internal object HideNavBarBadgesFingerprint : Fingerprint(
    definingClass = "/BadgeableTabView;",
    name = "setBadgeNumber",
)

/** FollowNudgeButtonViewDelegateBinder – nudge (follow/subscribe) buttons. */
internal object HideNudgeButtonFingerprint : Fingerprint(
    definingClass = "FollowNudgeButtonViewDelegateBinder;",
    strings = listOf("viewDelegate", "viewModel"),
)

/** InlineActionView.setText(String, boolean) – inline metrics. */
internal object InlineActionViewTextFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/ui/tweet/inlineactions/InlineActionView;",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Z"),
)

/** TweetStatView setText – detailed metrics. */
internal object TweetStatViewTextFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/ui/tweet/",
    returnType = "V",
    parameters = listOf(
        "Lcom/twitter/ui/tweet/TweetStatView;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
    ),
)

/** SocialProofView.setSocialProofData. */
internal object HideSocialProofFingerprint : Fingerprint(
    definingClass = "SocialProofView;",
    name = "setSocialProofData",
)

/** Premium upsell feature flag check. */
internal object RemovePremiumUpsellFingerprint : Fingerprint(
    filters = listOf(string("subscriptions_upsells_premium_home_nav")),
)

/** JsonSensitiveMediaWarning$$JsonObjectMapper.parse(). */
internal object SensitiveMediaFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/core/JsonSensitiveMediaWarning\$\$JsonObjectMapper;",
    name = "parse",
    returnType = "Ljava/lang/Object;",
)

/** Show post source label feature flag. */
internal object SourceLabelFingerprint : Fingerprint(
    filters = listOf(string("show_tweet_source_disabled")),
)

/** JsonCardInstanceData$$JsonObjectMapper.parseField – poll results. */
internal object JsonCardInstanceDataFingerprint : Fingerprint(
    definingClass = "JsonCardInstanceData\$\$JsonObjectMapper;",
    name = "parseField",
    filters = listOf(string("binding_values")),
)

/** DisableAutoScroll: app launch tracker. */
internal object DisableAutoScrollFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf(
        "applicationManager", "releaseCompletable", "preferences",
        "twSystemClock", "launchTracker", "cold_start_launch_time_millis",
    ),
)

/** Video auto-advance threshold. */
internal object EnableVidAutoAdvanceFingerprint : Fingerprint(
    filters = listOf(
        string("immersive_video_auto_advance_duration_threshold"),
        opcode(Opcode.MOVE_RESULT),
    ),
)

/** Player support class for forcing HD. */
internal object PlayerSupportFingerprint : Fingerprint(
    definingClass = "/av/player/support/",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    custom = { paramCount(2) },
)

/** JsonProfileRecommendationModuleResponse – recommended users. */
internal object HideRecommendedUsersFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/people/JsonProfileRecommendationModuleResponse;",
    filters = listOf(opcode(Opcode.IGET_OBJECT)),
)

/** Community membership model (badge). */
internal object CommunityModelFingerprint : Fingerprint(
    strings = listOf("actionResults", "role"),
)
