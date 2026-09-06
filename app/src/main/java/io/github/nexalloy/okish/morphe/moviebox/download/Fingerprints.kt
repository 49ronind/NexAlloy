package io.github.nexalloy.okish.morphe.moviebox.download

import io.github.nexalloy.morphe.Fingerprint

object ParallelDownloadLimitFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/member/MemberProvider;",
    returnType = "I",
    strings = listOf("kv_parallel_download_task_num"),
)
