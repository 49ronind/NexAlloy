package io.github.nexalloy.revanced.facebook.ad

import io.github.nexalloy.morphe.findMethodDirect
import java.lang.reflect.Modifier

val reelsInstreamAdBreakParserFingerprint = findMethodDirect {
    val ownerName = findMethod {
        matcher { usingStrings("ReelsInstreamAdBreaks") }
    }.firstNotNullOfOrNull { it.declaredClass?.name }
        ?: error("Unable to resolve the Reels player params builder")

    findMethod {
        matcher {
            modifiers = Modifier.STATIC
            returnType = "com.google.common.collect.ImmutableList"
            paramTypes("com.facebook.graphql.model.GraphQLMedia")
        }
    }.firstOrNull { it.declaredClass?.name == ownerName }
        ?: error("Unable to resolve the Reels instream ad-break parser")
}
