package io.github.nexalloy.okish.morphe.moviebox.update

import io.github.nexalloy.morphe.Fingerprint

object ForceUpdateFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/version/update/RemoteVersionInfo;",
    name = "getForceUpdate",
    returnType = "Z",
)

object HasUpdateFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/version/update/RemoteVersionInfo;",
    name = "getHasUpdate",
    returnType = "Z",
)
