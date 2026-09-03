package io.github.nexalloy.rushiranpise.morphe.adguard.premium

import io.github.nexalloy.patch

val UnlockLifetimePremium = patch(
    name = "Unlock Lifetime Premium",
    description = "Unlocks AdGuard Lifetime Premium (Family) features.",
) {
    var fakePaidLicense: Any? = null

    fun getPaidLicense(): Any? {
        if (fakePaidLicense != null) return fakePaidLicense
        return runCatching {
            val constructor = PaidLicenseFingerprint.constructor
            val paidLicenseClass = PaidLicenseFingerprint.declaredClass

            // Second constructor param is the LicenseType enum class
            val licenseTypeClass = constructor.parameterTypes.getOrNull(1) ?: return null
            val familyEnum = licenseTypeClass.enumConstants?.firstOrNull { (it as? Enum<*>)?.name == "Family" }
                ?: licenseTypeClass.enumConstants?.firstOrNull { (it as? Enum<*>)?.name == "Personal" }
                ?: licenseTypeClass.enumConstants?.firstOrNull()

            // LicenseDuration.Lifetime singleton
            val lifetimeClass = LifetimeDurationFingerprint.declaredClass
            val lifetimeInstance = lifetimeClass.declaredFields.firstOrNull {
                java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == lifetimeClass
            }?.apply { isAccessible = true }?.get(null)

            constructor.isAccessible = true

            // PaidLicense(licenseKey="", licenseType=Family, licenseDuration=Lifetime, devCount=1, maxDevCount=9, keyOwner="")
            constructor.newInstance("", familyEnum, lifetimeInstance, 1, 9, "").also {
                fakePaidLicense = it
            }
        }.getOrNull()
    }

    // Layer 2: getCachedPlusState()
    runCatching {
        GetPlusStateFingerprint.hookMethod {
            before { param ->
                val license = getPaidLicense()
                if (license != null) {
                    param.result = license
                }
            }
        }
    }

    // Layer 3: setPlusState(incoming)
    runCatching {
        SetPlusStateFingerprint.hookMethod {
            before { param ->
                val license = getPaidLicense()
                if (license != null) {
                    param.args[0] = license
                }
            }
        }
    }

    // Layer 4: fetchAndUpdatePlusState()
    runCatching {
        StateFlowResolverFingerprint.hookMethod {
            before { param ->
                val license = getPaidLicense()
                if (license != null) {
                    param.result = license
                }
            }
        }
    }

    // Layer 5: fetchPlusStateForPromo()
    runCatching {
        PromoStateFlowResolverFingerprint.hookMethod {
            before { param ->
                val license = getPaidLicense()
                if (license != null) {
                    param.result = license
                }
            }
        }
    }

    // Layer 6: activateLicenseKey() -> skip backend re-verification
    runCatching {
        LicenseKeyActivateFingerprint.hookMethod {
            before { param ->
                param.result = null
            }
        }
    }
}
