package io.github.nexalloy.epxec.morphe.freereels.premium

import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.patch

val EnableVipAndDownload = patch(
    name = "Enable VIP and no-ads download",
    description = "Enables VIP features and allows downloading episodes without ads.",
) {
    runCatching {
        FreereelsVipFingerprint.hookMethod {
            before { param ->
                param.result = true
            }
        }
    }

    runCatching {
        FreereelsDownloadAdsFingerprint.hookMethod {
            after { param ->
                runCatching {
                    XposedHelpers.setBooleanField(param.thisObject, "g0", false)
                }
            }
        }
    }
}
