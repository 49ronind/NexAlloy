package io.github.nexalloy.revanced.facebook.profileads

import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findMethodDirect

/**
 * Fingerprints for the "Sponsored · Not connected to <friend>" ads that Facebook
 * injects into a friend's profile timeline. Reverse-engineered from Facebook for
 * Android (July 2026 build).
 *
 *   Surface identifier .............. "facebook_profile_ads_brand_safety" (QE)
 *   Renderer .......................... "TimelineStoryComponentSpec"
 *   Sponsored payload field ........... "is_non_connected_page_post"
 *
 * These ads travel the same feed CSR / story-pool pipeline that the main
 * HideFacebookAds patch already hooks — but the underlying GraphQL Sponsored
 * object is materialised through a different chokepoint:
 *
 *     ReactMarketplaceVideoAdsUtils : Sponsored data builder
 *         └── LX/R5p;->A01(String) : LX/3yW;
 *             (parses the JSON payload with "is_non_connected_page_post" +
 *              "show_sponsored_label" and returns a GraphQL Sponsored tree)
 *
 * Nulling out the return of that builder starves every downstream consumer —
 * feed CSR, sponsored pool, TimelineStoryComponentSpec impression logging —
 * and takes the "Not connected to <friend>" variant with it.
 */

// ─── Sponsored data builder (LX/R5p;->A01) ────────────────────────────────────
//
// Uniquely identifiable by the union of these three strings which appear
// nowhere else together:
//   - "is_non_connected_page_post"
//   - "show_sponsored_label"
//   - "Failed to create Graph QL Sponsored data"
// Signature: static, takes a single String (JSON), returns a boxed tree wrapper
// (LX/3yW; — a TreeJNI subclass in the current build).

val sponsoredDataBuilderMethodFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings(
                "is_non_connected_page_post",
                "show_sponsored_label",
                "Failed to create Graph QL Sponsored data",
            )
            paramTypes("java.lang.String")
        }
    }.first()
}

// ─── Profile ads brand-safety QE gate (LX/1Iq;->A0P) ──────────────────────────
//
// Facebook checks the "facebook_profile_ads_brand_safety" mobile-config gate
// before letting the profile ad reach TimelineStoryComponentSpec. Method sig:
//     static int A0P(String qeName, int defaultValue)
// Returning the default (0 = disabled) turns the surface off for this user.
//
// The method is one big string-switch across ~thousands of QE names, uniquely
// matched by the combination of this QE and one nearby feed logging QE.

val profileAdsBrandSafetyQeMethodFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("facebook_profile_ads_brand_safety", "fb_story_ads_hide_ad")
            returnType = "int"
            paramTypes("java.lang.String", "int")
        }
    }.first()
}

// ─── TimelineStoryComponentSpec ads impression method (LX/9c8;->A1K) ──────────
//
// The Litho component-spec entry point that renders a profile-timeline story.
// It short-circuits its side-effects when the current story is an ad-brand-
// safety impression — we replicate the short-circuit unconditionally so the
// side-effect (adding to seen-ad HashSet + writing telemetry) never fires and
// no impression bubbles up.
//
// Uniquely matched by the pair "TimelineStoryComponentSpec" + surface tag
// "facebook_profile_ads_brand_safety" + "native_timeline" — no other class in
// the DEX carries all three.

val timelineStoryComponentSpecClassFingerprint = findClassDirect {
    findClass {
        matcher {
            usingStrings(
                "TimelineStoryComponentSpec",
                "facebook_profile_ads_brand_safety",
                "native_timeline",
            )
        }
    }.single()
}
