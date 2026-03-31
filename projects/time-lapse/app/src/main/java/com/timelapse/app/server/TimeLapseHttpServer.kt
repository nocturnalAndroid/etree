package com.timelapse.app.server

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.timelapse.app.SessionConfig
import com.timelapse.app.SessionManager
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.File

class TimeLapseHttpServer(private val context: Context) : NanoHTTPD(8080) {

    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return try {
            when {
                method == Method.GET && uri == "/" -> serveAsset("web/index.html", "text/html")
                method == Method.GET && uri == "/style.css" -> serveAsset("web/style.css", "text/css")
                method == Method.GET && uri == "/app.js" -> serveAsset("web/app.js", "application/javascript")
                method == Method.GET && uri == "/api/status" -> handleGetStatus()
                method == Method.POST && uri == "/api/session/start" -> handleSessionStart(session)
                method == Method.POST && uri == "/api/session/stop" -> handleSessionStop()
                method == Method.GET && uri == "/api/last-photo" -> handleLastPhoto()
                method == Method.POST && uri == "/api/settings" -> handleSettings(session)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            }.also { response ->
                response.addHeader("Access-Control-Allow-Origin", "*")
            }
        } catch (e: Exception) {
            val errorJson = gson.toJson(mapOf("error" to (e.message ?: "Internal server error")))
            jsonResponse(Response.Status.INTERNAL_ERROR, errorJson)
        }
    }

    private fun serveAsset(assetPath: String, mimeType: String): Response {
        return try {
            val inputStream = context.assets.open(assetPath)
            val bytes = inputStream.readBytes()
            inputStream.close()
            newFixedLengthResponse(Response.Status.OK, mimeType, ByteArrayInputStream(bytes), bytes.size.toLong())
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Asset not found: $assetPath")
        }
    }

    private fun handleGetStatus(): Response {
        val status = SessionManager.getStatus()
        val cameras = SessionManager.getAvailableCameras(context)
        val ntfyTopic = SessionManager.getNtfyTopic(context)

        val json = JsonObject().apply {
            addProperty("isRunning", status.isRunning)
            addProperty("photoCount", status.photoCount)
            addProperty("startTimeMs", status.startTimeMs)
            addProperty("nextCaptureMs", status.nextCaptureMs)
            addProperty("lastJpegPath", status.lastJpegPath)
            addProperty("uploadPending", status.uploadPending)
            addProperty("uploadFailed", status.uploadFailed)
            addProperty("driveSessionFolder", status.driveSessionFolder)
            addProperty("ntfyTopic", ntfyTopic)
            add("cameras", gson.toJsonTree(cameras))
        }

        return jsonResponse(Response.Status.OK, gson.toJson(json))
    }

    private fun handleSessionStart(session: IHTTPSession): Response {
        val body = readBody(session)
        val json = gson.fromJson(body, JsonObject::class.java)

        val intervalMs = json.get("intervalMs")?.asLong
            ?: return jsonResponse(Response.Status.BAD_REQUEST, """{"error":"missing intervalMs"}""")
        val cameraId = json.get("cameraId")?.asString
            ?: return jsonResponse(Response.Status.BAD_REQUEST, """{"error":"missing cameraId"}""")
        val wbMode = json.get("wbMode")?.asString
            ?: return jsonResponse(Response.Status.BAD_REQUEST, """{"error":"missing wbMode"}""")

        SessionManager.startSession(context, SessionConfig(intervalMs, cameraId, wbMode))
        return jsonResponse(Response.Status.OK, """{"ok":true}""")
    }

    private fun handleSessionStop(): Response {
        SessionManager.stopSession(context)
        return jsonResponse(Response.Status.OK, """{"ok":true}""")
    }

    private fun handleLastPhoto(): Response {
        val file: File? = SessionManager.getLastJpegFile()
        return if (file != null && file.exists()) {
            val bytes = file.readBytes()
            newFixedLengthResponse(Response.Status.OK, "image/jpeg", ByteArrayInputStream(bytes), bytes.size.toLong())
                .also { it.addHeader("Access-Control-Allow-Origin", "*") }
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "No photo available")
        }
    }

    private fun handleSettings(session: IHTTPSession): Response {
        val body = readBody(session)
        val json = gson.fromJson(body, JsonObject::class.java)
        val ntfyTopic = json.get("ntfyTopic")?.asString
            ?: return jsonResponse(Response.Status.BAD_REQUEST, """{"error":"missing ntfyTopic"}""")
        SessionManager.setNtfyTopic(context, ntfyTopic)
        return jsonResponse(Response.Status.OK, """{"ok":true}""")
    }

    private fun readBody(session: IHTTPSession): String {
        val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
        val buffer = ByteArray(contentLength)
        session.inputStream.read(buffer, 0, contentLength)
        return String(buffer, Charsets.UTF_8)
    }

    private fun jsonResponse(status: Response.Status, json: String): Response {
        return newFixedLengthResponse(status, "application/json", json)
            .also { it.addHeader("Access-Control-Allow-Origin", "*") }
    }
}
