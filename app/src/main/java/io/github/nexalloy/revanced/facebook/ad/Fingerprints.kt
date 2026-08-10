package io.github.nexalloy.revanced.facebook.ad

import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.result.MethodData
import java.lang.reflect.Modifier

/**
 * Mirrors upstream's post-resolution filter used in resolveFeedCsrFilterMethods,
 * resolveLateFeedListHooks and resolveStoryPoolAddMethods: excludes constructors and
 * any method that is abstract, or declared on an interface/abstract class, since
 * those can't be hooked directly — Xposed needs the concrete implementing method.
 * DexKit's MethodData/ClassData expose `modifiers` directly from the dex, so this
 * can run entirely at fingerprint-resolution time (no classLoader needed).
 */
private fun MethodData.isConcreteHookTarget(): Boolean {
    if (isConstructor || Modifier.isAbstract(modifiers)) return false
    val ownerModifiers = declaredClass?.modifiers ?: return true
    return !Modifier.isInterface(ownerModifiers) && !Modifier.isAbstract(ownerModifiers)
}

// ─── Ad-kind enum ─────────────────────────────────────────────────────────────

val adKindEnumFingerprint = findClassDirect {
    findClass {
        matcher { usingEqStrings("AD", "UGC", "PARADE", "MIDCARD") }
    }.first()
}

// ─── Reels list-builder ───────────────────────────────────────────────────────
// Primary: class that logs "Non ads story fall into ads rendering logic"
// Fallback: structural signature (static 6-param void + static 5-param ArrayList)

val listBuilderClassFingerprint = findClassDirect {
    // Primary: structural — the class must contain methods matching ALL 6 shapes
    // below (mirrors upstream resolveListBuilderClass's `methods { Contains }` check
    // exactly). Only trusted when it resolves to a SINGLE unambiguous class.
    val structural = findClass {
        matcher {
            methods {
                matchType = MatchType.Contains
                add {
                    modifiers = Modifier.STATIC
                    returnType = "void"
                    paramTypes(null, null, null, null, null, "java.util.List")
                }
                add {
                    returnType = "void"
                    paramTypes(null, null, null, null, null, "java.util.List")
                }
                add {
                    modifiers = Modifier.STATIC
                    returnType = "java.util.ArrayList"
                    paramTypes(null, null, null, null, "boolean")
                }
                add {
                    modifiers = Modifier.STATIC
                    returnType = "java.util.ArrayList"
                    paramTypes(null, null, null, null, null, "boolean")
                }
                add {
                    returnType = "java.util.ArrayList"
                    paramTypes(null, null, null, "java.lang.Iterable")
                }
                add {
                    returnType = "java.util.List"
                    paramTypes(null, null, null, "boolean")
                }
            }
        }
    }

    // Fallback: string-based — only consulted when the structural search above is
    // ambiguous (0 or 2+ matches), exactly mirroring upstream's
    // `structuralCandidates.singleOrNull() ?: batchCandidates.firstOrNull() ?: error(...)`.
    structural.singleOrNull()
        ?: findClass {
            matcher { usingStrings("Non ads story fall into ads rendering logic, StoryType=%s, StoryId=%s") }
        }.firstOrNull()
        ?: error("Unable to resolve the upstream Facebook reels list-builder class")
}

// NOTE: listBuilderAppendFingerprint / listBuilderFactoryFingerprint were removed.
// Upstream now resolves these two methods via plain reflection + a scoring heuristic
// over every method on the already-resolved listBuilderClass (no rigid param-shape
// match), because Facebook occasionally ships variants with a different parameter
// count/order. That scoring logic needs a real java.lang.reflect.Method (List
// subtype checks via Class.isAssignableFrom), which only exists once classLoader is
// available — see resolveListBuilderAppendMethod / resolveListBuilderFactoryMethod
// in FacebookAdHelpers.kt, called from the patch body with
// ::listBuilderClassFingerprint.clazz (still DexKit-cached) as input.

// ─── Plugin packs ─────────────────────────────────────────────────────────────
// Upstream now blocks BOTH FbShortsViewerPluginPack AND MarketplaceAdsPluginPack.

val pluginPackMethodsFingerprint = findMethodListDirect {
    listOf("FbShortsViewerPluginPack", "MarketplaceAdsPluginPack").flatMap { tag ->
        findClass {
            matcher {
                methods {
                    add { returnType = "java.lang.String"; paramCount = 0; usingStrings(tag) }
                    add { returnType = "java.util.List"; paramCount = 0 }
                }
            }
        }.flatMap { cls ->
            cls.findMethod { matcher { returnType = "java.util.List"; paramCount = 0 } }
        }
    }.distinctBy { it.descriptor }.filter { !it.isConstructor }
}

// ─── Instream banner eligibility ─────────────────────────────────────────────
// Upstream resolves the CLASS first via a structural "0-arg String-returning method
// that uses this tag" shape (findClassesByZeroArgStringTags), then picks the actual
// boolean()/0-param eligibility method via plain reflection — preferring a non-static
// method declared on/inherited by that class, falling back to walking the superclass
// chain if none is found directly. That second part needs a real Class<*>
// (classLoader), so it lives in resolveInstreamBannerEligibilityMethod in
// FacebookAdHelpers.kt, called from the patch body with this class as input.

val instreamBannerEligibilityClassFingerprint = findClassDirect {
    findClass {
        matcher {
            methods {
                matchType = MatchType.Contains
                add { returnType = "java.lang.String"; paramCount = 0; usingStrings("InstreamAdIdleWithBannerState") }
            }
        }
    }.firstOrNull() ?: error("Unable to resolve the instream banner eligibility class")
}

// ─── Indicator pill eligibility ──────────────────────────────────────────────
// Upstream requires the CLASS to use BOTH strings (the render-path string and the
// fully-qualified plugin class name), then finds the static boolean(3-param) method
// inside that class — it doesn't require the method itself to reference either string.

val indicatorPillAdEligibilityFingerprint = findMethodDirect {
    val candidates = findClass {
        matcher {
            usingStrings(
                "IndicatorPillComponent.render",
                "com.facebook.feedback.comments.plugins.indicatorpill.reelsadsfloatingcta.ReelsAdsFloatingCtaPlugin"
            )
        }
    }
    candidates.firstNotNullOfOrNull { cls ->
        cls.findMethod {
            findFirst = true
            matcher { modifiers = Modifier.STATIC; returnType = "boolean"; paramCount = 3 }
        }.firstOrNull()
    } ?: error("Unable to resolve the Reels indicator pill ad eligibility method")
}

// ─── Reels banner render methods ─────────────────────────────────────────────

val reelsBannerRenderMethodsFingerprint = findMethodListDirect {
    listOf("ReelsBannerAdsComponent", "ReelsBannerAdsNativeComponent").flatMap { tag ->
        findMethod {
            matcher { paramCount = 1; usingStrings(tag) }
        }.filter { m -> !m.isConstructor }
    }.distinctBy { it.descriptor }
}

// ─── Profile Reels async ad query ─────────────────────────────────────────────

val profileReelsAsyncAdsQueryFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            paramTypes(
                "com.facebook.auth.usersession.FbUserSession",
                "java.lang.Integer",
                "java.lang.Integer",
                "boolean"
            )
            usingStrings("ProfileReelsAsyncAdsQuery")
        }
    }.first { !it.isConstructor }
}

// ─── Feed CSR cache filter ────────────────────────────────────────────────────
// Upstream now also matches a newer 4-param variant — (FbUserSession, ?, ImmutableList, int) —
// in addition to the original 3-param (FbUserSession, ImmutableList, int) shape.
// We search both shapes per candidate class; HideFacebookAdsPatch derives the correct
// listArgIndex afterwards from each resolved Method's real parameter types.

val feedCsrFilterMethodsFingerprint = findMethodListDirect {
    listOf("FeedCSRCacheFilter", "FeedCSRCacheFilter2025H1", "FeedCSRCacheFilter2026H1", "FeedCSRCacheFilter2026H2").flatMap { tag ->
        findClass {
            matcher { usingStrings(tag) }
        }.flatMap { cls ->
            // NOTE: older builds returned the filtered ImmutableList directly. Current
            // builds return a result WRAPPER instead — e.g.
            //   AnH(FbUserSession, <ctx>, ImmutableList, int) -> LX/2iE
            // where the filtered list sits in a field of that wrapper. Pinning
            // returnType to ImmutableList therefore matched NOTHING and the whole feed
            // CSR filter hook silently never installed (runCatching swallowed it),
            // which is why sponsored items still reached the profile feed.
            // We no longer constrain the return type at all; the hook only needs the
            // ImmutableList PARAMETER, which it rewrites in beforeHookedMethod. The
            // param shape plus the class-level tag string is specific enough.
            val fourParam = cls.findMethod {
                matcher {
                    paramTypes(
                        "com.facebook.auth.usersession.FbUserSession",
                        null,
                        "com.google.common.collect.ImmutableList",
                        "int"
                    )
                }
            }
            if (fourParam.isNotEmpty()) fourParam else cls.findMethod {
                matcher {
                    paramTypes(
                        "com.facebook.auth.usersession.FbUserSession",
                        "com.google.common.collect.ImmutableList",
                        "int"
                    )
                }
            }
        }
    }.distinctBy { it.descriptor }.filter { it.isConcreteHookTarget() }
}

// ─── Late feed list sanitisers ────────────────────────────────────────────────

val lateFeedListMethodsFingerprint = findMethodListDirect {
    val results = ArrayList<org.luckypray.dexkit.result.MethodData>()

    findClass { matcher { usingStrings("handleStorageStories", "Empty Storage List") } }.forEach { cls ->
        cls.findMethod {
            matcher { returnType = "void"; paramTypes(null, "com.google.common.collect.ImmutableList", "int") }
        }.forEach { results.add(it) }
    }

    findClass { matcher { usingStrings("cancelVendingTimerAndAddToPool_") } }.forEach { cls ->
        cls.findMethod {
            matcher { returnType = "void"; paramTypes("com.google.common.collect.ImmutableList", "java.lang.String") }
        }.forEach { results.add(it) }
    }

    listOf("CSRNoOpStorageLifecycleImpl", "FeedCSRStorageLifecycle", "FriendlyFeedCSRStorageLifecycle", "FbShortsCSRStorageLifecycle").forEach { tag ->
        findClass { matcher { usingStrings(tag) } }.forEach { cls ->
            // 3-param variant, e.g. AAB(FbUserSession, <ctx>, ImmutableList)
            cls.findMethod {
                matcher {
                    returnType = "void"
                    paramTypes("com.facebook.auth.usersession.FbUserSession", null, "com.google.common.collect.ImmutableList")
                }
            }.forEach { results.add(it) }
            // 4-param variant, e.g. AAA(FbUserSession, <ctx>, <ctx>, ImmutableList).
            // Present on the FriendlyFeed (professional-mode profile) lifecycle and
            // previously unhooked, letting sponsored stories through on that surface.
            cls.findMethod {
                matcher {
                    returnType = "void"
                    paramTypes("com.facebook.auth.usersession.FbUserSession", null, null, "com.google.common.collect.ImmutableList")
                }
            }.forEach { results.add(it) }
            // 1-param variant, e.g. AFq(ImmutableList) on the FriendlyFeed lifecycle.
            cls.findMethod {
                matcher {
                    returnType = "void"
                    paramTypes("com.google.common.collect.ImmutableList")
                }
            }.forEach { results.add(it) }
        }
    }

    results.distinctBy { it.descriptor }.filter { it.isConcreteHookTarget() }
}

// ─── Story pool add ───────────────────────────────────────────────────────────

val storyPoolAddMethodsFingerprint = findMethodListDirect {
    listOf("CSRStoryPoolCoordinator", "FeedStoryPoolCoordinator").flatMap { tag ->
        findClass { matcher { usingStrings(tag) } }.flatMap { cls ->
            cls.findMethod { matcher { returnType = "boolean"; paramCount = 1 } }
        }
    }.distinctBy { it.descriptor }.filter { it.isConcreteHookTarget() }
}

// ─── Sponsored pool ───────────────────────────────────────────────────────────
// Upstream requires the CLASS to use BOTH strings, then verifies the
// boolean(GraphQLFeedUnitEdge) method shape exists somewhere in that class.

val sponsoredPoolClassFingerprint = findClassDirect {
    val candidates = findClass {
        matcher { usingEqStrings("SponsoredPoolContainerAdapter", "Edge type mismatch; not added") }
    }
    candidates.firstOrNull { cls ->
        cls.findMethod {
            matcher { returnType = "boolean"; paramTypes("com.facebook.graphql.model.GraphQLFeedUnitEdge") }
        }.isNotEmpty()
    } ?: error("Unable to resolve the Facebook sponsored pool class")
}

val sponsoredPoolAddMethodFingerprint = findMethodDirect {
    sponsoredPoolClassFingerprint().findMethod {
        matcher { returnType = "boolean"; paramTypes("com.facebook.graphql.model.GraphQLFeedUnitEdge") }
    }.single()
}

// ─── Sponsored story manager ──────────────────────────────────────────────────
// Upstream requires the CLASS to use BOTH strings, then verifies the
// GraphQLFeedUnitEdge()/0-param method shape exists somewhere in that class.

val sponsoredStoryManagerClassFingerprint = findClassDirect {
    val candidates = findClass {
        matcher { usingEqStrings("FeedSponsoredStoryHolder.onPositionReset", "freshFeedStoryHolder") }
    }
    candidates.firstOrNull { cls ->
        cls.findMethod {
            matcher { returnType = "com.facebook.graphql.model.GraphQLFeedUnitEdge"; paramCount = 0 }
        }.isNotEmpty()
    } ?: error("Unable to resolve the Facebook sponsored story manager class")
}

val sponsoredStoryNextMethodFingerprint = findMethodDirect {
    sponsoredStoryManagerClassFingerprint().findMethod {
        matcher { returnType = "com.facebook.graphql.model.GraphQLFeedUnitEdge"; paramCount = 0 }
    }.single()
}

// ─── Story ads in-disc source ─────────────────────────────────────────────────
// Upstream changed search string to "ads_deletion" (from commit fixing profile timeline ads)

val storyAdsInDiscClassFingerprint = findClassDirect {
    findMethod {
        matcher { usingStrings("ads_deletion") }
    }.first { md ->
        val cls = md.declaredClass ?: return@first false
        cls.findMethod {
            matcher {
                returnType = "com.google.common.collect.ImmutableList"
                paramTypes("com.facebook.auth.usersession.FbUserSession", null, "com.google.common.collect.ImmutableList")
            }
        }.isNotEmpty() && cls.findMethod {
            matcher { returnType = "void"; paramTypes(null, "com.google.common.collect.ImmutableList") }
        }.isNotEmpty()
    }.declaredClass!!
}

/**
 * The specific 0-param void method inside storyAdsInDiscClass that triggers ad insertion.
 * Upstream finds this via usingStrings("ads_insertion") — we replicate that here.
 */
val storyAdsInsertionTriggerMethodFingerprint = findMethodDirect {
    storyAdsInDiscClassFingerprint().findMethod {
        matcher {
            returnType = "void"
            paramCount = 0
            usingStrings("ads_insertion")
        }
    }.firstOrNull()
        ?: storyAdsInDiscClassFingerprint().findMethod {
            // Fallback: first 0-param void method if string not found (obfuscated builds)
            matcher { returnType = "void"; paramCount = 0 }
        }.first()
}

// ─── Game ad request methods ──────────────────────────────────────────────────

val gameAdRequestMethodsFingerprint = findMethodListDirect {
    listOf(
        "Invalid JSON content received by onGetInterstitialAdAsync: ",
        "Invalid JSON content received by onGetRewardedInterstitialAsync: ",
        "Invalid JSON content received by onRewardedVideoAsync: ",
        "Invalid JSON content received by onLoadAdAsync: ",
        "Invalid JSON content received by onShowAdAsync: "
    ).flatMap { tag ->
        findMethod {
            matcher { returnType = "void"; paramTypes("org.json.JSONObject"); usingStrings(tag) }
        }
    }.distinctBy { it.descriptor }.filter { !it.isConstructor }
}

// ─── Feed collection edge filter ──────────────────────────────────────────────
// Replaces FB571_FEED_COLLECTION_TARGETS (was pinned to X.1vr). "addNewEdgeToCollection"
// is one of the very few feed methods that survives ProGuard with its real name, so it
// can be matched by name + shape on any build. Verified on FB 573:
//   X.1vy.addNewEdgeToCollection(ImmutableList$Builder, GraphQLFeedUnitEdge, X.1cS): boolean
val feedCollectionAddEdgeMethodFingerprint = findMethodDirect {
    val byShape = findMethod {
        matcher {
            name = "addNewEdgeToCollection"
            returnType = "boolean"
            paramTypes(null, "com.facebook.graphql.model.GraphQLFeedUnitEdge", null)
        }
    }.filter { it.isConcreteHookTarget() }

    byShape.firstOrNull()
        // Looser fallback: any concrete addNewEdgeToCollection that takes an edge
        // somewhere in its parameter list (param count/order occasionally shifts).
        ?: findMethod {
            matcher { name = "addNewEdgeToCollection"; returnType = "boolean" }
        }.first {
            it.isConcreteHookTarget() &&
                it.paramTypeNames.any { p -> p == "com.facebook.graphql.model.GraphQLFeedUnitEdge" }
        }
}

// ─── Story ad source providers (all of them) ──────────────────────────────────
// Upstream pinned SIX provider classes by name (FB571_STORY_AD_SOURCE_CLASSES) because
// the single-class DexKit lookup missed the split pipelines. This returns EVERY class
// that both logs "ads_deletion" and carries the provider shape, so no name is needed.
// Verified on FB 573: three classes log "ads_deletion", exactly one carries the shape.
val storyAdsInDiscMethodsFingerprint = findMethodListDirect {
    findMethod {
        matcher { usingStrings("ads_deletion") }
    }.filter { md ->
        val cls = md.declaredClass ?: return@filter false
        cls.findMethod {
            matcher {
                returnType = "com.google.common.collect.ImmutableList"
                paramTypes("com.facebook.auth.usersession.FbUserSession", null, "com.google.common.collect.ImmutableList")
            }
        }.isNotEmpty() && cls.findMethod {
            matcher { returnType = "void"; paramTypes(null, "com.google.common.collect.ImmutableList") }
        }.isNotEmpty()
    }.distinctBy { it.declaredClass?.name }
}
