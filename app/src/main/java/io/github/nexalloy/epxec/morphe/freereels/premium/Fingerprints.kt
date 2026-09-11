package io.github.nexalloy.epxec.morphe.freereels.premium

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess

object FreereelsVipFingerprint : Fingerprint(
    definingClass = "Lcom/dramawave/shared/models/bean/WalletBean;",
    name = "M",
    returnType = "Z",
    parameters = emptyList(),
)

object FreereelsDownloadAdsFingerprint : Fingerprint(
    definingClass = "Lcom/dramawave/feature/home/download/viewmodel/VideoDownloadViewModel;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Lcom/dramawave/service/api/repository/HomeRepository;",
        "Lcom/dramawave/feature/home/download/reward/DownloadResolutionRewardScheduler;",
        "Landroidx/lifecycle/SavedStateHandle;",
    ),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_BOOLEAN,
            definingClass = "this",
            name = "g0",
            type = "Z",
        )
    ),
)
