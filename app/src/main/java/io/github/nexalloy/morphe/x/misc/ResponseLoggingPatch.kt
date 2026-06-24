package io.github.nexalloy.morphe.x.misc

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * Logs JSON responses received from X servers to /sdcard/Android/data/<pkg>/files/piko_response.json.
 * Mirrors piko's ResponseLoggingPatch: wraps the Jackson InputStream factory.
 */
val ResponseLogging = patch(
    name = "Log server response",
    description = "Logs JSON responses from X servers for debugging.",
    use = false,
) {
    dependsOn(XVersionCheck)

    InputStreamFingerprint.hookMethod {
        before { param ->
            val inputStream = param.args.filterIsInstance<InputStream>().firstOrNull()
                ?: return@before
            try {
                val bytes = inputStream.readBytes()
                val logDir = appContext.getExternalFilesDir(null) ?: return@before
                val logFile = File(logDir, "nexalloy_x_response.json")
                logFile.appendText(String(bytes) + "\n---\n")
                Logger.printDebug { "ResponseLogging: wrote ${bytes.size} bytes" }
                // Replace the stream so Jackson can still read it
                param.args[param.args.indexOfFirst { it is InputStream }] =
                    ByteArrayInputStream(bytes)
            } catch (_: Exception) {}
        }
    }
}
