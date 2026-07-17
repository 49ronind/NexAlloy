package io.github.nexalloy.revanced.facebook.profileads

/**
 * Profile-timeline ads use plain-old GraphQL delivery — no React/JSON parser
 * involved — so this patch does NOT use DexKit fingerprints. Every target is
 * a stable, non-obfuscated symbol under com.facebook.graphql.model.* and is
 * looked up directly via classLoader.loadClass at hook-install time inside
 * HideProfileTimelineAdsPatch.
 *
 * The chokepoints, and why each was chosen:
 *
 *   Lcom/facebook/graphql/model/GraphQLStory;->A0N()LX/3yW;
 *       Canonical getSponsoredData(). The single non-obfuscated field name
 *       "sponsored_data" — grepped across all 20 DEX files — resolves to
 *       exactly one accessor of the correct shape. Every downstream check
 *       ("is this story an ad?") funnels through it, including:
 *
 *   LX/2o6;->A02(GraphQLStory):Z
 *       isSponsoredStory(). Its entire body reduces to
 *          return story != null && story.A0N() != null
 *       so once A0N returns null the whole ecosystem — Litho render specs
 *       (TimelineStoryComponentSpec / LX/9c8;), impression logging
 *       (FeedUnitImpressionLoggerController.logSponsoredImpression),
 *       overlay label render (LX/YXm;->A0i "ads_sponsored_label"), etc. —
 *       naturally skips the sponsored path.
 *
 * The previous version of this patch hooked LX/R5p;->A01, which is only the
 * *React* Marketplace video-ads JSON→GraphQL builder. Profile-timeline ads
 * arrive as pre-materialised GraphQL trees from the server response and
 * bypass R5p entirely — that is why the earlier patch left the ad visible.
 */
