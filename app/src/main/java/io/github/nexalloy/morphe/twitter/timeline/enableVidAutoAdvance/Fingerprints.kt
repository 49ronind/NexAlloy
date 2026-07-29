package io.github.nexalloy.morphe.twitter.timeline.enableVidAutoAdvance

import io.github.nexalloy.morphe.Fingerprint

/**
 * Matches the integer feature-flag getter for the immersive video
 * auto-advance duration threshold. Piko swaps the MOVE_RESULT value right
 * after the flag lookup call; we achieve the same outcome by overwriting
 * the method's own result after it returns.
 */
internal object EnableVidAutoAdvanceFingerprint : Fingerprint(
    strings = listOf("immersive_video_auto_advance_duration_threshold"),
)
