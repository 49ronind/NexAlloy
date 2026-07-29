package io.github.nexalloy.morphe.twitter.timeline.hideHiddenReplies

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint

/**
 * Model class name is used for JSON deserialization and is not obfuscated.
 * Piko targets the last boolean field read in a method returning `Object`
 * (most likely `<init>`); we target the constructor directly since that is
 * where the field is populated right after parsing.
 */
internal object HideHiddenRepliesFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/timeline/urt/JsonTimelineTweet;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
)
