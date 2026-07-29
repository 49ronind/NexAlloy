package io.github.nexalloy.morphe.twitter.timeline.live

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint

internal object HideLiveThreadsFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/fleets/api/json/JsonFleetsTimelineResponse;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
)
