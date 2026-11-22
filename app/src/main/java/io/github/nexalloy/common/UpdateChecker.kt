package io.github.nexalloy.common

import android.R
import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.Utils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import de.robv.android.xposed.XposedHelpers
import fuel.Fuel
import fuel.get
import io.github.nexalloy.BuildConfig
import io.github.nexalloy.hookMethod
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.readString
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

data class ReleaseInfo(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body_html") val releaseNoteHtml: String,
    @SerializedName("html_url") val releaseUrl: String,
    @SerializedName("assets") val assets: List<ReleaseAsset> = emptyList()
) {
    fun getUpdateVersion(
        checkBetaUpdates: Boolean,
        currentVersionCode: Int,
        currentCommit: String
    ): String? {
        if (checkBetaUpdates) {
            val apkNamePattern = Regex("""NexAlloy-(.+)\.apk""")
            val commit = checkNotNull(assets.mapNotNull {
                apkNamePattern.matchEntire(it.name)?.groupValues?.get(1)
            }.singleOrNull()) { "Expected exactly one NexAlloy APK asset in autobuild" }
            return "Nightly build ($commit)".takeIf { commit != currentCommit }
        }

        val version = VersionInfo.fromTagName(tagName)
        return version.versionName.takeIf { version.versionCode > currentVersionCode }
    }
}

data class ReleaseAsset(@SerializedName("name") val name: String)

data class VersionInfo(val versionCode: Int, val versionName: String) {
    companion object {
        fun fromTagName(tagName: String): VersionInfo {
            val versionCode: Int
            val versionName: String

            val split = tagName.split('-', limit = 2)
            if (split.count() == 2) {
                // VersionCode-VersionName
                versionCode = split[0].toIntOrNull() ?: 0
                versionName = split[1]
            } else {
                // X.Y.Z, Z is versionCode
                versionCode = tagName.split('.').last().toIntOrNull() ?: 0
                versionName = tagName
            }
            return VersionInfo(versionCode, versionName)
        }
    }
}

const val OWNER = "NexAlloy"
const val REPO = "NexAlloy"
const val currentVersionCode = BuildConfig.VERSION_CODE
const val CHECK_BETA_UPDATES = "check_beta_updates"

class UpdateChecker(private val checkBetaUpdates: () -> Boolean = { false }) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + CoroutineExceptionHandler { _, err ->
            Logger.printException({ "coroutineContext error" }, err)
        }

    private var currentActivity = WeakReference<Activity>(null)

    var runOnce = false

    fun setActivity(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    fun hookNewActivity() {

        XposedHelpers.findMethodExact(
            Instrumentation::class.java,
            "newActivity",
            ClassLoader::class.java,
            String::class.java,
            Intent::class.java
        ).hookMethod {
            after {
                if (!runOnce) {
                    runOnce = true
                    currentActivity = WeakReference(it.result as Activity)
                    autoCheckUpdate()
                }
            }
        }
    }

    fun autoCheckUpdate() {
        if (Random.nextInt(0, 10) != 0) return
        Logger.printInfo { "start auto check update." }
        runCatching { checkUpdate() }
    }

    fun checkUpdate(silent: Boolean = true) {
        launch {
            try {
                val betaUpdates = checkBetaUpdates()
                val releasePath = if (betaUpdates) "tags/autobuild" else "latest"
                val response = Fuel.get(
                    "https://api.github.com/repos/$OWNER/$REPO/releases/$releasePath",
                    headers = mapOf("Accept" to "application/vnd.github.html+json")
                )
                if (response.statusCode != 200) {
                    Logger.printException { "Failed to fetch latest release: HTTP ${response.statusCode}" }
                    return@launch
                }

                val content = response.source.readString()
                Logger.printDebug { content }
                val release = Gson().fromJson(content, ReleaseInfo::class.java)
                val updateVersion = release.getUpdateVersion(
                    betaUpdates, currentVersionCode, BuildConfig.COMMIT_HASH
                )
                if (updateVersion != null) {
                    Logger.printInfo { "Found new version of NexAlloy $updateVersion" }
                    showUpdateDialog(release, updateVersion)
                } else {
                    Logger.printInfo { "no update found for NexAlloy" }
                    if (!silent) Utils.showToastLong("NexAlloy is up to date.")
                }
            } catch (e: Throwable) {
                Logger.printException({ "checkUpdate error" }, e)
            }
        }
    }

    @Deprecated("Test only.")
    fun showRelease(version: String) {
        launch {
            val response = Fuel.get(
                "https://api.github.com/repos/$OWNER/$REPO/releases/tags/$version",
                headers = mapOf("Accept" to "application/vnd.github.html+json")
            )
            if (response.statusCode != 200) {
                Logger.printException { "responseCode ${response.statusCode}" }
                return@launch
            }

            val content = response.source.readString()
            Logger.printDebug { content }

            val release = Gson().fromJson(content, ReleaseInfo::class.java)
            showUpdateDialog(release, VersionInfo.fromTagName(release.tagName).versionName)
        }
    }

    fun requireActivity() = currentActivity.get()!!

    private fun showUpdateDialog(release: ReleaseInfo, versionName: String) {
        launch(Dispatchers.Main) {
            try {
                val theme =
                    if (Utils.isDarkModeEnabled()) R.style.Theme_DeviceDefault_Dialog_Alert
                    else R.style.Theme_DeviceDefault_Light_Dialog_Alert
                val dialog = AlertDialog.Builder(requireActivity(), theme)
                    .setTitle("Found new version of NexAlloy $versionName")
                    .setMessage(
                        Html.fromHtml(release.releaseNoteHtml, Html.FROM_HTML_MODE_LEGACY)
                    ).setPositiveButton(R.string.ok) { _, _ ->
                        openReleasePage(release.releaseUrl)
                    }.setNegativeButton(requireActivity().getString(R.string.cancel), null)
                    .create()
                dialog.show()
                dialog.findViewById<TextView>(R.id.message).movementMethod =
                    LinkMovementMethod.getInstance()
            } catch (e: Throwable) {
                Logger.printException({ "showUpdateDialog error" }, e)
            }
        }
    }

    private fun openReleasePage(releaseUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            requireActivity().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Utils.showToastLong(e.message.toString())
        }
    }
}
