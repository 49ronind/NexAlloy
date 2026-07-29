package io.github.nexalloy.morphe.twitter.misc.searchsuggestions

import io.github.nexalloy.morphe.Fingerprint

internal object SearchDbInsertFingerprint : Fingerprint(
    strings = listOf(
        "search_queries",
        "findSearchQuery: ",
        "LOWER(query)=LOWER(?) AND LOWER(name)=LOWER(?) AND type=? AND latitude=? AND longitude=?",
    ),
)

internal object SearchSuggestionFingerprint : Fingerprint(
    returnType = "Ljava/util/Collection;",
    strings = listOf(
        "type",
        "query_id",
    ),
)
