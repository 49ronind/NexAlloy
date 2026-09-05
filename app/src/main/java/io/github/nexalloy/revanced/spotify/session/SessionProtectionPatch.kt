package io.github.nexalloy.revanced.spotify.session

import android.content.SharedPreferences
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch

private object AuthCache {
    @Volatile var body: String? = null
    @Volatile var contentType: Any? = null
}

val SessionProtection = patch(
    name = "Session protection",
    description = "Prevents forced logouts by caching token refreshes, blocking dual-sync detection endpoints, and safeguarding stored credentials.",
) {
    // LAYER 1: Token refresh response caching & replay
    runCatching {
        val chainClass = Class.forName("okhttp3.Interceptor\$Chain", false, classLoader)
        val reqClass = Class.forName("okhttp3.Request", false, classLoader)
        val respClass = Class.forName("okhttp3.Response", false, classLoader)
        val bodyClass = Class.forName("okhttp3.ResponseBody", false, classLoader)
        val mtClass = Class.forName("okhttp3.MediaType", false, classLoader)
        val urlClass = Class.forName("okhttp3.HttpUrl", false, classLoader)
        val builderClass = Class.forName("okhttp3.Response\$Builder", false, classLoader)

        val reqUrl = reqClass.getMethod("url")
        val urlHost = urlClass.getMethod("host")
        val urlPath = urlClass.getMethod("encodedPath")
        val respCode = respClass.getMethod("code")
        val respReq = respClass.getMethod("request")
        val respBody = respClass.getMethod("body")
        val respNewBuilder = respClass.getMethod("newBuilder")
        val bodyStr = bodyClass.getMethod("string")
        val bodyCT = bodyClass.getMethod("contentType")
        val bodyCreate = bodyClass.getMethod("create", mtClass, String::class.java)
        val bCode = builderClass.getMethod("code", Int::class.java)
        val bMsg = builderClass.getMethod("message", String::class.java)
        val bBody = builderClass.getMethod("body", bodyClass)
        val bBuild = builderClass.getMethod("build")

        val peekBody = runCatching {
            respClass.getMethod("peekBody", Long::class.javaPrimitiveType)
        }.getOrNull()

        val proceed = chainClass.getMethod("proceed", reqClass)
        XposedBridge.hookMethod(proceed, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val resp = param.result ?: return
                    val req = respReq.invoke(resp) ?: return
                    val url = reqUrl.invoke(req) ?: return
                    val host = urlHost.invoke(url) as? String ?: return
                    val path = urlPath.invoke(url) as? String ?: ""

                    val isAuth = host.contains("login5") ||
                        (host.endsWith(".spotify.com") && (
                            path.contains("/v3/login") ||
                            path.contains("/token") ||
                            path.contains("/auth/token")
                        ))
                    if (!isAuth) return

                    val code = respCode.invoke(resp) as Int

                    if (code in 200..299) {
                        try {
                            val text: String?
                            if (peekBody != null) {
                                val peeked = peekBody.invoke(resp, 65536L)
                                text = bodyStr.invoke(peeked) as? String
                            } else {
                                val b = respBody.invoke(resp) ?: return
                                val ct = bodyCT.invoke(b)
                                text = bodyStr.invoke(b) as? String
                                if (text != null) {
                                    val nb = bodyCreate.invoke(null, ct, text)
                                    val rb = respNewBuilder.invoke(resp)
                                    bBody.invoke(rb, nb)
                                    param.result = bBuild.invoke(rb)
                                }
                            }
                            if (text != null && text.contains("access_token")) {
                                AuthCache.body = text
                                runCatching {
                                    val b = respBody.invoke(if (peekBody != null) resp else param.result)
                                    if (b != null) AuthCache.contentType = bodyCT.invoke(b)
                                }
                            }
                        } catch (_: Throwable) {}
                    } else if (code == 401 || code == 403) {
                        val cached = AuthCache.body
                        if (cached != null) {
                            val ct = AuthCache.contentType
                            val replayBody = bodyCreate.invoke(null, ct, cached)
                            val rb = respNewBuilder.invoke(resp)
                            bCode.invoke(rb, 200)
                            bMsg.invoke(rb, "OK")
                            bBody.invoke(rb, replayBody)
                            param.result = bBuild.invoke(rb)
                        }
                    }
                } catch (_: Throwable) {}
            }
        })
    }

    // LAYER 2: Block detection/sync endpoints
    runCatching {
        val chainClass = Class.forName("okhttp3.Interceptor\$Chain", false, classLoader)
        val reqClass = Class.forName("okhttp3.Request", false, classLoader)
        val bodyClass = Class.forName("okhttp3.ResponseBody", false, classLoader)
        val builderClass = Class.forName("okhttp3.Response\$Builder", false, classLoader)
        val protocolClass = Class.forName("okhttp3.Protocol", false, classLoader)
        val mtClass = Class.forName("okhttp3.MediaType", false, classLoader)
        val urlClass = Class.forName("okhttp3.HttpUrl", false, classLoader)

        val reqUrl = reqClass.getMethod("url")
        val urlHost = urlClass.getMethod("host")
        val urlPath = urlClass.getMethod("encodedPath")
        val newBuilder = builderClass.getConstructor()
        val bReq = builderClass.getMethod("request", reqClass)
        val bProto = builderClass.getMethod("protocol", protocolClass)
        val bCode = builderClass.getMethod("code", Int::class.java)
        val bMsg = builderClass.getMethod("message", String::class.java)
        val bBody = builderClass.getMethod("body", bodyClass)
        val bBuild = builderClass.getMethod("build")
        val http11 = protocolClass.getField("HTTP_1_1").get(null)
        val parseMT = mtClass.getMethod("parse", String::class.java)
        val textMT = parseMT.invoke(null, "text/plain")
        val bodyCreate = bodyClass.getMethod("create", mtClass, String::class.java)

        val detectionPaths = setOf(
            "/v3/dual-sync/",
            "/dual-sync/",
            "/v1/social-connect/",
            "/melody/v1/check"
        )

        fun isSpclient(host: String): Boolean {
            val h = host.lowercase()
            return h.contains("spclient") && h.endsWith(".spotify.com")
        }

        fun isDetectionPath(path: String): Boolean {
            val p = path.lowercase()
            return detectionPaths.any { p.startsWith(it) }
        }

        val proceed = chainClass.getMethod("proceed", reqClass)
        XposedBridge.hookMethod(proceed, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    val req = param.args[0] ?: return
                    val url = reqUrl.invoke(req) ?: return
                    val host = urlHost.invoke(url) as? String ?: return
                    val path = urlPath.invoke(url) as? String ?: return

                    if (isSpclient(host) && isDetectionPath(path)) {
                        val emptyBody = bodyCreate.invoke(null, textMT, "")
                        val builder = newBuilder.newInstance()
                        bReq.invoke(builder, req)
                        bProto.invoke(builder, http11)
                        bCode.invoke(builder, 204)
                        bMsg.invoke(builder, "Blocked by NexAlloy")
                        bBody.invoke(builder, emptyBody)
                        param.result = bBuild.invoke(builder)
                    }
                } catch (_: Throwable) {}
            }
        })
    }

    // LAYER 3: Protect stored auth credentials
    runCatching {
        val protectedKeywords = setOf(
            "access_token", "refresh_token", "token",
            "session", "auth", "credential", "login",
            "bearer", "oauth", "account"
        )

        fun isAuthKey(key: String?): Boolean {
            if (key == null) return false
            val k = key.lowercase()
            return protectedKeywords.any { k.contains(it) }
        }

        val editorClass = SharedPreferences.Editor::class.java

        val removeMethod = editorClass.getMethod("remove", String::class.java)
        XposedBridge.hookMethod(removeMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val key = param.args[0] as? String ?: return
                if (isAuthKey(key)) {
                    param.result = param.thisObject
                }
            }
        })

        val clearMethod = editorClass.getMethod("clear")
        XposedBridge.hookMethod(clearMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = param.thisObject
            }
        })
    }
}
