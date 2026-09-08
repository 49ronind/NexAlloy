package io.github.nexalloy.rushiranpise.morphe.dramabox.membership

import io.github.nexalloy.morphe.Fingerprint

// ─── DIRECT TARGETS (MOVIEBOX / PROTONVPN PATTERN) ──────────────────────────

// Z6/dramabox.s2()Z — runtime VIP state getter (DataStore read).
// Checked by billing, membership UI, and ad playback guards (e.g. InterstitialAdVM.lO).
// Returning true unconditionally makes all client-side VIP checks succeed.
object VipStateGetterFingerprint : Fingerprint(
    definingClass = "LZ6/dramabox;",
    name = "s2",
    returnType = "Z",
)

// Z6/dramabox.E7(Z)V — runtime VIP state setter (DataStore write).
// Called from user info sync and billing callbacks to persist VIP state.
// Mutating parameter 0 to true forces DataStore to always persist VIP=true.
object VipStateSetterFingerprint : Fingerprint(
    definingClass = "LZ6/dramabox;",
    name = "E7",
    returnType = "V",
    parameters = listOf("Z"),
)

// x9/switch.l1(DramaPurchase)V — billing success handler.
// Called after Google Play Billing acknowledges a purchase.
// Suppressing this prevents non-VIP coin purchases from clearing VIP state.
object BillingSuccessFingerprint : Fingerprint(
    definingClass = "Lx9/switch;",
    name = "l1",
    returnType = "V",
    parameters = listOf("Lcom/lib/recharge/bean/DramaPurchase;"),
)
