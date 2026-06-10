package io.github.nexalloy.revanced.instagram.ads

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

val feedAdsProxyFetcherFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("FeedAdsProxyFetcher.processSponsoredContentInPayload")
        }
    }.single()
}

val clipsAdPrewarmFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("ClipsViewerAdapter.prewarmSponsoredItem")
        }
    }.single()
}

val clipsAdAddItemFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("ClipsViewerAdapter.addClipsItems:addToDataSource")
        }
    }.single()
}

val storiesAdsBinderFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("HybridStoriesAdsLithoBinder#bindView")
        }
    }.single()
}

val paidPartnershipLabelFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("PaidPartnershipLabelConfigImpl")
        }
    }.single()
}

val gridSponsoredPoolFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("GridSponsoredPoolItem(sponsoredContent=")
        }
    }.single()
}

val adV2ControllerClassFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("SponsoredContentControllerV2.onCurrentIndexChanged")
        }
    }.first()
}

val adV2DeliveryFingerprint = findMethodDirect {
    val v2ClassName = adV2ControllerClassFingerprint().declaredClassName
    findMethod {
        matcher {
            declaredClass(v2ClassName)
            usingStrings("onSponsoredContentDelivered")
            paramCount = 7
        }
    }.first()
}

val adV2InsertGateFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "boolean"
            usingStrings("injection_orchestrator_position_passed_with_insertion_but_not_impression_")
        }
    }.single()
}
