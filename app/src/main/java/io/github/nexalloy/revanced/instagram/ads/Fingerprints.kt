package io.github.nexalloy.revanced.instagram.ads

import io.github.nexalloy.morphe.findMethodDirect

val feedAcpContentInjectorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("FeedAcp.createNewController:contentInjector")
        }
    }.first()
}

val feedAdsProxyFetcherFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("FeedAdsProxyFetcher.processSponsoredContentInPayload")
        }
    }.single()
}

val adControllerIndexFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("SponsoredContentController.onCurrentIndexChanged")
        }
    }.first()
}

val adDeliveredFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("Must implement onSponsoredContentDelivered with poolInsertionType")
        }
    }.single()
}

val adContentDeliveredExternallyFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("onContentDeliveredExternally")
        }
    }.first()
}

val adInsertGateFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "boolean"
            usingStrings("injection_orchestrator_position_passed_with_insertion_but_not_impression_")
        }
    }.single()
}

val adHighestPositionGateFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "boolean"
            usingStrings("injection_orchestrator_highest_position_push_up_")
        }
    }.first()
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

val sponsoredReelItemBinderFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("SponsoredReelViewerItemBinder#bindView")
        }
    }.single()
}

val sponsoredReelMediaFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("SponsoredReelViewerItemBinder#bindMedia")
        }
    }.single()
}

val sponsoredReelLabelFooterFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("SponsoredReelSponsoredLabelFooterBinder#bindView")
        }
    }.single()
}

val sponsoredReelLabelOnBottomFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("SponsoredReelSponsoredLabelOnMediaBottomBinder#bindView")
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

val storiesAdsPrepareFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("HybridStoriesAdsLithoBinder#prepare")
        }
    }.single()
}

val sponsoredStoriesLikeButtonFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("SponsoredStoriesLikeButtonBinder#bindView")
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
