package io.github.nexalloy.revanced.threads.ads

import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint

val adFetchSponsoredContentFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("onFetchSponsoredContent")
        }
    }.single()
}

val adContentDeliveredFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("onSponsoredContentDelivered")
        }
    }.single()
}

val paidPartnershipLabelFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("PaidPartnershipLabel (PaidPartnershipLabel.kt:25)")
        }
    }.single()
}

val adMetadataFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("AdMetadata (AdMetadata.kt:54)")
        }
    }.single()
}

val sponsoredLabelInHeaderFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("SponsoredLabelInHeader (SponsoredLabel.kt:12)")
        }
    }.single()
}

val spoolAdInjectorLambdaFingerprint = fingerprint {
    definingClass("Lcom/instagram/barcelona/feed/data/cache/BarcelonaSpoolFeedCacheHandler\$injectAdToFeedSessionAtPosition\$1;")
    name("invokeSuspend")
}
