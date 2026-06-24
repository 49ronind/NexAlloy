package io.github.nexalloy.morphe.x.misc

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

/** X-Lite redirect flag check. */
internal object RedirectingToXLiteFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf(
        "x_lite_in_tfa_for_existing_users_enabled",
        "existing_user_redirected_to_x_lite",
        "x_lite_in_tfa_for_existing_users_exit_enabled",
    ),
)

/** XChat unified tab user-ID threshold check. */
internal object XchatSubSystemUserCheckFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("userId", "xchat_unified_tab_min_snowflake_user_id"),
)

/** FAB visibility (android_compose_fab_menu_enabled). */
internal object HideFABFingerprint : Fingerprint(
    filters = listOf(string("android_compose_fab_menu_enabled")),
)

/** Search-DB insert (for pausing/removing suggestions). */
internal object SearchDbInsertFingerprint : Fingerprint(
    strings = listOf(
        "search_queries",
        "findSearchQuery: ",
        "LOWER(query)=LOWER(?) AND LOWER(name)=LOWER(?) AND type=? AND latitude=? AND longitude=?",
    ),
)

/** Search suggestion provider. */
internal object SearchSuggestionFingerprint : Fingerprint(
    definingClass = "/search/provider/",
    returnType = "Ljava/util/Collection;",
    strings = listOf("type", "query_id"),
)

/** Number formatting (abbr_number_divider_*). */
internal object RoundOffNumbersFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf(
        "abbr_number_divider_billions",
        "abbr_number_divider_millions",
        "abbr_number_divider_thousands",
    ),
)

/** JsonProfileRecommendationModuleResponse – recommended users popup. */
internal object HideRecommendedUsersFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/people/JsonProfileRecommendationModuleResponse;",
    filters = listOf(opcode(Opcode.IGET_OBJECT)),
)

/** MainActivity super class – used to hook changelogs display. */
internal object MainActivityFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/app/main/MainActivity;",
)

/** Login activity constructor – for import/export token. */
internal object OcfCtaStepDynamicFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/onboarding/ocf/common/",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
)

/** Jackson InputStream factory – for response logging. */
internal object InputStreamFingerprint : Fingerprint(
    definingClass = "/fasterxml/jackson/core/",
    parameters = listOf("Ljava/io/InputStream;"),
    custom = { m, _ -> m.returnType.contains("/fasterxml/jackson/core/") },
)

/** emoji2 CharSequence hook for custom font. */
internal object CustomFontHookFingerprint : Fingerprint(
    definingClass = "emoji2/text",
    filters = listOf(string("end should be < than charSequence length")),
)
