package io.github.nexalloy.revanced.spotify.ads

import android.app.Activity
import android.os.Bundle
import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.net.InetAddress

val InterceptAds = patch(
    name = "Intercept ads",
    description = "Suppresses audio, display, and video ads via DNS sinkholing, OkHttp path interception, and UI dismissal.",
) {
    val blockedDomains = setOf(
        "ads.spotify.com",
        "audio-ak-spotify-com.akamaized.net",
        "audio2.spotify.com",
        "adstats.spotify.com",
        "adeventtracker.spotify.com",
        "sponsored-recommendations.spotify.com",
        "desktop.spotify.com",
        "weblb-wg.gslb.spotify.com",
        "redirect.spotify.net",
        "spclient.wg.spotify.com",
        "analytics.spotify.com",
        "tracking.spotify.com",
        "log.spotify.com",
        "crashdump.spotify.com",
        "dealer.g2.spotify.com",
        "gew1-dealer.g2.spotify.com",
        "gew1-dealer-ssl.spotify.com",
        "firebaseinstallations.googleapis.com",
        "firebase-settings.crashlytics.com",
        "cdn.branch.io",
        "api2.branch.io",
        "pagead2.googlesyndication.com",
        "bs.serving-sys.com",
        "bounceexchange.com",
        "sb.scorecardresearch.com",
        "b.scorecardresearch.com",
        "segment-data-us-east.zqtk.net",
        "live.ravelin.click",
    )

    val blockedPathPrefixes = setOf(
        "/ads/",
        "/ad-logic/",
        "/ad-monetization/",
        "/v1/ads/",
        "/v2/ads/",
        "/v3/ads/",
        "/ads?"
    )

    fun isSpclientDomain(host: String?): Boolean {
        if (host == null) return false
        val h = host.lowercase()
        return h.contains("spclient") && h.endsWith(".spotify.com")
    }

    fun isBlockedPath(path: String?): Boolean {
        if (path == null) return false
        val p = path.lowercase()
        return blockedPathPrefixes.any { prefix -> p.startsWith(prefix) }
    }

    val loopback = InetAddress.getByAddress("blocked.local", byteArrayOf(127, 0, 0, 1))
    val loopbackArray = arrayOf(loopback)

    fun isBlocked(host: String?): Boolean {
        if (host == null) return false
        val h = host.lowercase()
        return blockedDomains.any { domain ->
            h == domain || h.endsWith(".$domain")
        }
    }

    // 1A: DNS getAllByName
    runCatching {
        val method = InetAddress::class.java.getMethod("getAllByName", String::class.java)
        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val host = param.args[0] as? String ?: return
                if (isBlocked(host)) {
                    param.result = loopbackArray
                }
            }
        })
    }

    // 1B: DNS getByName
    runCatching {
        val method = InetAddress::class.java.getMethod("getByName", String::class.java)
        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val host = param.args[0] as? String ?: return
                if (isBlocked(host)) {
                    param.result = loopback
                }
            }
        })
    }

    // 2A: URL openConnection fallback
    runCatching {
        val urlClass = java.net.URL::class.java
        val openConn = urlClass.getMethod("openConnection")
        XposedBridge.hookMethod(openConn, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val url = param.thisObject as? java.net.URL ?: return
                if (isBlocked(url.host)) {
                    param.throwable = java.io.IOException("Blocked: ${url.host}")
                }
            }
        })
    }

    // 2B: OkHttp Path Interception
    runCatching {
        val chainClass = Class.forName("okhttp3.Interceptor\$Chain", false, classLoader)
        val reqClass = Class.forName("okhttp3.Request", false, classLoader)
        val respBuilderClass = Class.forName("okhttp3.Response\$Builder", false, classLoader)
        val protocolClass = Class.forName("okhttp3.Protocol", false, classLoader)
        val responseBodyClass = Class.forName("okhttp3.ResponseBody", false, classLoader)
        val mediaTypeClass = Class.forName("okhttp3.MediaType", false, classLoader)
        val httpUrlClass = Class.forName("okhttp3.HttpUrl", false, classLoader)

        val urlMethod = reqClass.getMethod("url")
        val hostMethod = httpUrlClass.getMethod("host")
        val encodedPathMethod = httpUrlClass.getMethod("encodedPath")

        val newBuilder = respBuilderClass.getConstructor()
        val builderRequest = respBuilderClass.getMethod("request", reqClass)
        val builderProtocol = respBuilderClass.getMethod("protocol", protocolClass)
        val builderCode = respBuilderClass.getMethod("code", Int::class.java)
        val builderMessage = respBuilderClass.getMethod("message", String::class.java)
        val builderBody = respBuilderClass.getMethod("body", responseBodyClass)
        val builderBuild = respBuilderClass.getMethod("build")

        val http11 = protocolClass.getField("HTTP_1_1").get(null)
        val parseMediaType = mediaTypeClass.getMethod("parse", String::class.java)
        val emptyMediaType = parseMediaType.invoke(null, "text/plain")
        val createBody = responseBodyClass.getMethod("create", mediaTypeClass, String::class.java)

        val proceedMethod = chainClass.getMethod("proceed", reqClass)
        XposedBridge.hookMethod(proceedMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    val request = param.args[0] ?: return
                    val httpUrl = urlMethod.invoke(request) ?: return
                    val host = hostMethod.invoke(httpUrl) as? String ?: return
                    val path = encodedPathMethod.invoke(httpUrl) as? String ?: return

                    if (isSpclientDomain(host) && isBlockedPath(path)) {
                        val emptyBody = createBody.invoke(null, emptyMediaType, "")
                        val builder = newBuilder.newInstance()
                        builderRequest.invoke(builder, request)
                        builderProtocol.invoke(builder, http11)
                        builderCode.invoke(builder, 204)
                        builderMessage.invoke(builder, "Blocked by NexAlloy")
                        builderBody.invoke(builder, emptyBody)
                        param.result = builderBuild.invoke(builder)
                    }
                } catch (_: Throwable) {}
            }
        })
    }

    // 3: OkHttp DNS Interceptor
    runCatching {
        val dnsInterface = Class.forName("okhttp3.Dns", false, classLoader)
        val systemDnsField = dnsInterface.getField("SYSTEM")
        val systemDns = systemDnsField.get(null)
        if (systemDns != null) {
            val lookupMethod = systemDns.javaClass.getMethod("lookup", String::class.java)
            XposedBridge.hookMethod(lookupMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val host = param.args[0] as? String ?: return
                    if (isBlocked(host)) {
                        param.result = listOf(loopback)
                    }
                }
            })
        }
    }

    // 4A: Dismiss DisplayAdActivity
    runCatching {
        val displayAdClass = Class.forName("com.spotify.adsdisplay.display.DisplayAdActivity", false, classLoader)
        val onCreateMethod = displayAdClass.getDeclaredMethod("onCreate", Bundle::class.java)
        onCreateMethod.isAccessible = true
        XposedBridge.hookMethod(onCreateMethod, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                (param.thisObject as? Activity)?.finish()
            }
        })
    }

    // 4B: Dismiss InAppBrowserActivity
    runCatching {
        val browserClass = Class.forName("com.spotify.adsdisplay.browser.inapp.InAppBrowserActivity", false, classLoader)
        val onCreateMethod = browserClass.getDeclaredMethod("onCreate", Bundle::class.java)
        onCreateMethod.isAccessible = true
        XposedBridge.hookMethod(onCreateMethod, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                (param.thisObject as? Activity)?.finish()
            }
        })
    }

    // 4C: PlayerState adBreakContext skip
    runCatching {
        val builderClass = Class.forName("com.spotify.player.model.AutoValue_PlayerState\$Builder", false, classLoader)
        val adBreakMethod = builderClass.declaredMethods.firstOrNull { it.name == "adBreakContext" && it.parameterCount == 1 }
        if (adBreakMethod != null) {
            XposedBridge.hookMethod(adBreakMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = param.thisObject
                }
            })
        }
    }

    // 4D: PlayerState deserializeAdBreakContext null
    runCatching {
        val deserializerClass = Class.forName("com.spotify.player.model.PlayerState_Deserializer", false, classLoader)
        val deserializeMethod = deserializerClass.declaredMethods.firstOrNull { it.name == "deserializeAdBreakContext" }
        if (deserializeMethod != null) {
            XposedBridge.hookMethod(deserializeMethod, XC_MethodReplacement.returnConstant(null))
        }
    }
}
