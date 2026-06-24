package io.github.nexalloy.morphe.x.misc

import android.widget.TextView
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Makes bio and username text selectable on profile pages.
 * Mirrors piko's SelectableTextPatch which sets android:textIsSelectable via XML.
 * In Xposed we hook the TypefacesTextView bind calls that inflate profile_details.xml.
 */
val SelectableText = patch(
    name = "Selectable Text",
    description = "Makes bio and username text on profiles selectable.",
) {
    dependsOn(XVersionCheck)

    // Hook setTextIsSelectable on the custom TextView used by X for user_name and user_bio.
    // We can't reference TypefacesTextView directly without the app's classes, so we hook
    // Activity.onResume and make any bio/username TextViews selectable.
    // A lighter approach: hook View.setId and when it's the user_bio / user_name id, flip selectable.
    // Simplest portable approach: hook TypefacesTextView constructor.
    try {
        val clazz = XposedHelpers.findClassIfExists(
            "com.twitter.ui.components.text.legacy.TypefacesTextView",
            classLoader
        )
        if (clazz != null) {
            // Hook all constructors and make text selectable after init
            for (ctor in clazz.declaredConstructors) {
                de.robv.android.xposed.XposedBridge.hookMethod(ctor,
                    object : de.robv.android.xposed.XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            (param.thisObject as? TextView)?.setTextIsSelectable(true)
                        }
                    })
            }
        }
    } catch (_: Exception) {}
}
