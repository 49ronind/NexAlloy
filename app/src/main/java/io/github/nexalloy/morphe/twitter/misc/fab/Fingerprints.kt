package io.github.nexalloy.morphe.twitter.misc.fab

import io.github.nexalloy.morphe.Fingerprint

/**
 * Confirmed via DEX analysis: Lcom/twitter/android/app/fab/f;->a(...)
 * is the actual FAB provider factory method. It always builds and
 * returns a FloatingActionButton wrapper unless it returns null - which
 * only happens when the injected preference check short-circuits at the
 * very top (piko's insertion point). Matching by class + return type is
 * enough since this method is the sole "a" in this class.
 */
internal object FabProviderFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/android/app/fab/f;",
    returnType = "Lcom/twitter/ui/fab/u;",
)
