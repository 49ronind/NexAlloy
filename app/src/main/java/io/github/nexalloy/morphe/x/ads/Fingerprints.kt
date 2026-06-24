package io.github.nexalloy.morphe.x.ads

import io.github.nexalloy.morphe.Fingerprint

/**
 * JsonTimelineEntry$$JsonObjectMapper.parse()
 * Fingerprint: defines class + return type uniquely identifies the mapper.
 */
internal object TimelineEntryParseFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/timeline/urt/JsonTimelineEntry\$\$JsonObjectMapper;",
    name = "parse",
    returnType = "Ljava/lang/Object;",
)

/**
 * JsonTimelineModuleItem$$JsonObjectMapper.parse()
 */
internal object TimelineModuleItemParseFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/timeline/urt/JsonTimelineModuleItem\$\$JsonObjectMapper;",
    name = "parse",
    returnType = "Ljava/lang/Object;",
)

/**
 * JsonTimelineTrend return-object method (for promoted-trend filtering).
 */
internal object HidePromotedTrendFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/timeline/urt/JsonTimelineTrend;",
    returnType = "Ljava/lang/Object;",
)
