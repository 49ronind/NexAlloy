package io.github.nexalloy.revanced.instagram.ads

import io.github.nexalloy.morphe.findMethodDirect

val adInjectorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            usingStrings("SponsoredContentController.processValidatedContent")
        }
    }.single()
}

val adSponsoredContentFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "boolean"
            usingStrings("SponsoredContentController.insertItem")
        }
    }.single()
}

val adV2ControllerClassFingerprint = findMethodDirect {
    findMethod {
        matcher {
            usingStrings("SponsoredContentControllerV2.onCurrentIndexChanged")
        }
    }.first()
}

val adV2DeliveryFingerprint = findMethodDirect {
    val v2ClassName = adV2ControllerClassFingerprint().declaredClassName
    findMethod {
        matcher {
            declaredClass(v2ClassName)
            usingStrings("onSponsoredContentDelivered")
            paramCount = 7
        }
    }.first()
}

val adV2InsertGateFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "boolean"
            usingStrings("injection_orchestrator_position_passed_with_insertion_but_not_impression_")
        }
    }.single()
}
