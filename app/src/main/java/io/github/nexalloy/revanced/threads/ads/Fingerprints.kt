package io.github.nexalloy.revanced.threads.ads

import io.github.nexalloy.morphe.findMethodDirect

val adInjectorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("SponsoredContentController.processValidatedContent")
        }
    }.single()
}

val adSponsoredContentFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "boolean"
            usingStrings("SponsoredContentController.insertItem")
        }
    }.single()
}

val adInsertionActionFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("SponsoredContentController.processInsertionAction")
        }
    }.single()
}

val adContentDeliveredFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("SponsoredContentController.onSponsoredContentDelivered")
        }
    }.single()
}

val spoolAdInjectorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("injectAdToFeedSessionAtPosition")
        }
    }.single()
}

val paidPartnershipLabelFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("com.instagram.barcelona.feed.post.ui.PaidPartnershipLabel (PaidPartnershipLabel.kt:25)")
        }
    }.single()
}

val adMetadataFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("com.instagram.barcelona.sponsored.ui.AdMetadata (AdMetadata.kt:54)")
        }
    }.single()
}
