package io.github.nexalloy.rushiranpise.morphe.dramabox

import io.github.nexalloy.Patch
import io.github.nexalloy.rushiranpise.morphe.dramabox.lock.ActivationLockBypass
import io.github.nexalloy.rushiranpise.morphe.dramabox.membership.MembershipUnlock

val DramaBoxPatches = arrayOf<Patch>(
    ActivationLockBypass,
    MembershipUnlock,
)
