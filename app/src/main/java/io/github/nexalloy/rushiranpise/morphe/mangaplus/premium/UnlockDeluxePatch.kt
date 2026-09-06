package io.github.nexalloy.rushiranpise.morphe.mangaplus.premium

import io.github.nexalloy.patch

val UnlockDeluxe = patch(
    name = "Unlock Deluxe",
    description = "Unlocks Deluxe subscription client-side UI and navigation gates.",
) {
    runCatching {
        var cachedDeluxe: Any? = runCatching {
            SubscriptionPlanDeserializerFingerprint.method.invoke(null, "deluxe")
        }.getOrNull()

        SubscriptionPlanDeserializerFingerprint.hookMethod {
            before { param ->
                if (cachedDeluxe != null) {
                    param.result = cachedDeluxe
                } else {
                    param.args[0] = "deluxe"
                }
            }
            after { param ->
                if (cachedDeluxe == null && param.result != null) {
                    cachedDeluxe = param.result
                }
            }
        }
    }
}
