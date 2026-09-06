package io.github.nexalloy.okish.morphe.moviebox.download

import io.github.nexalloy.patch

val ParallelDownloads = patch(
    name = "Parallel downloads",
    description = "Allows up to 5 simultaneous downloads in MovieBox.",
) {
    ParallelDownloadLimitFingerprint.hookMethod {
        before { param ->
            param.result = 5
        }
    }
}
