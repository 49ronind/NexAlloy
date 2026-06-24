package io.github.nexalloy.morphe.x.link

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint

/**
 * AddSessionToken – the method that appends tracking params to shared URLs.
 * Parameters: (String url, SomeObject, String token) -> String
 */
internal object AddSessionTokenFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/String;", "L", "Ljava/lang/String;"),
    returnType = "Ljava/lang/String;",
    strings = listOf("<this>", "shareParam", "sessionToken"),
)

/**
 * JsonUrlEntity$$JsonObjectMapper.parse() – used to unshorten t.co URLs.
 */
internal object JsonUrlEntityParseFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/core/JsonUrlEntity\$\$JsonObjectMapper;",
    name = "parse",
    returnType = "Ljava/lang/Object;",
)

/**
 * UrlInterpreterActivity.onCreate – entry point for deep-link handling.
 */
internal object UrlInterpreterActivityCreateFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/deeplink/implementation/UrlInterpreterActivity;",
    name = "onCreate",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
)

/**
 * Modern share sheet link builder (post X 11.40).
 * First variant: contains "tweet-" and "https://x.com/i/status/".
 */
internal object NewShareSheetLinkFingerprint1 : Fingerprint(
    strings = listOf("tweet-", "https://x.com/i/status/"),
)

/**
 * Modern share sheet link builder – second variant.
 */
internal object NewShareSheetLinkFingerprint2 : Fingerprint(
    strings = listOf(
        "https://x.com/i/status/",
        "https://x.com/i/lists/",
        "https://x.com/i/trending/",
    ),
)
