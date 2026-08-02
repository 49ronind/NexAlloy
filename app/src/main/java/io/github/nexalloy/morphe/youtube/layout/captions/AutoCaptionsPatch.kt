package io.github.nexalloy.morphe.youtube.layout.captions

import app.morphe.extension.youtube.patches.AutoCaptionsPatch
import io.github.nexalloy.morphe.shared.misc.litho.filter.featureFlagCheck
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_26_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.onCreateHook
import io.github.nexalloy.patch

internal const val NO_VOLUME_CAPTIONS_FEATURE_FLAG = 45692436L

val AutoCaptions = patch(
    name = "Auto captions",
    description = "Adds an option to disable captions from being automatically enabled.",
) {
    dependsOn(VersionCheck)

    PreferenceScreen.PLAYER.addPreferences(
        if (is_20_26_or_greater) {
            ListPreference("morphe_auto_captions_style")
        } else {
            ListPreference(
                key = "morphe_auto_captions_style",
                entriesKey = "morphe_auto_captions_style_legacy_entries",
                entryValuesKey = "morphe_auto_captions_style_legacy_entry_values"
            )
        }
    )

    onCreateHook.add { AutoCaptionsPatch.newVideoStarted(it) }

    StartVideoInformerFingerprint.hookMethod {
        before { AutoCaptionsPatch.videoInformationLoaded() }
    }

    if (is_20_26_or_greater) {
        ::featureFlagCheck.hookMethod {
            before {
                // `(it.args[0] as Long)` compares primitives; `it.args[0] == 45692436L`
                // would box the constant into a fresh Long on every flag read.
                if ((it.args[0] as Long) == NO_VOLUME_CAPTIONS_FEATURE_FLAG) {
                    it.result = AutoCaptionsPatch.disableMuteAutoCaptions()
                }
            }
        }
    }
}
