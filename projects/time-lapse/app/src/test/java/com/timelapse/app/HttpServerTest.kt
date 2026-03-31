package com.timelapse.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.timelapse.app.server.TimeLapseHttpServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * E2E integration test for TimeLapseHttpServer.
 *
 * The server runs on a real TCP port (8081) using NanoHTTPD, and we hit it
 * with OkHttp — so this exercises the full HTTP parsing, routing, and JSON
 * serialisation stack without needing an Android device.
 *
 * Context is mocked because TimeLapseHttpServer only uses it for:
 *   1. context.assets.open(...)   — static file serving (tested elsewhere)
 *   2. SessionManager.*           — preference reads/writes via SharedPreferences
 *
 * NOTE: SessionManager is a Kotlin singleton, so state bleeds between tests.
 * We reset it to a known baseline in @Before via the public API surface.
 *
 * BUG FOUND: handleSessionStart calls SessionManager.startSession(context, config)
 * which internally calls ContextCompat.startForegroundService — this will throw
 * under a pure JVM test if Context is a real Android class. We work around it by
 * supplying a mock Context and verifying only the HTTP-level contract (status code
 * + JSON body), not the Service launch side-effect.
 */
class HttpServerTest {

    private val port = 8081
    private val baseUrl = "http://localhost:$port"
    private val client = OkHttpClient()

    private lateinit var server: TimeLapseHttpServer
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        // Build a SharedPreferences mock that can get/set the ntfy topic
        mockEditor = mock {
            on { putString(any(), any()) } doReturn mock
            on { apply() } doAnswer { Unit }
        }
        mockPrefs = mock {
            on { getString(eq(App.PREF_NTFY_TOPIC), any()) } doReturn "test-topic"
            on { edit() } doReturn mockEditor
        }
        mockContext = mock {
            on { getSharedPreferences(any(), any()) } doReturn mockPrefs
            on { packageName } doReturn "com.timelapse.app"
            // startService / startForegroundService should silently do nothing
            on { startService(any()) } doReturn null
            on { applicationContext } doReturn mock()
        }

        // Reset singleton state so tests don't interfere with each other
        SessionManager.updateNextCaptureMs(0L)
        // isRunning is @Volatile private — stopSession() sets it false without
        // actually needing to reach a real Service when we pass our mock context.
        SessionManager.stopSession(mockContext)

        server = TimeLapseHttpServer(mockContext)
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    // ─── GET /api/status ─────────────────────────────────────────────────────────

    @Test
    fun `GET status returns 200 with expected JSON fields`() {
        val response = get("/api/status")
        assertEquals(200, response.code)

        val json = response.body!!.string().parseJson()
        // All mandatory fields must be present
        assertNotNull("isRunning field missing", json.get("isRunning"))
        assertNotNull("photoCount field missing", json.get("photoCount"))
        assertNotNull("startTimeMs field missing", json.get("startTimeMs"))
        assertNotNull("nextCaptureMs field missing", json.get("nextCaptureMs"))
        assertNotNull("uploadPending field missing", json.get("uploadPending"))
        assertNotNull("uploadFailed field missing", json.get("uploadFailed"))
        assertNotNull("cameras field missing", json.get("cameras"))
        assertNotNull("ntfyTopic field missing", json.get("ntfyTopic"))
    }

    @Test
    fun `GET status returns CORS header`() {
        val response = get("/api/status")
        assertEquals("*", response.header("Access-Control-Allow-Origin"))
    }

    @Test
    fun `GET status reflects idle state after stop`() {
        val response = get("/api/status")
        val json = response.body!!.string().parseJson()
        assertFalse("Expected isRunning=false after stop", json.get("isRunning").asBoolean)
        assertEquals(0, json.get("photoCount").asInt)
    }

    // ─── POST /api/session/start ──────────────────────────────────────────────────

    @Test
    fun `POST session start with valid body returns ok=true`() {
        val body = """{"intervalMs":5000,"cameraId":"main","wbMode":"auto"}"""
        val response = postJson("/api/session/start", body)
        assertEquals(200, response.code)

        val json = response.body!!.string().parseJson()
        assertTrue("Expected ok=true", json.get("ok").asBoolean)
    }

    @Test
    fun `POST session start missing intervalMs returns 400`() {
        val body = """{"cameraId":"main","wbMode":"auto"}"""
        val response = postJson("/api/session/start", body)
        assertEquals(400, response.code)

        val json = response.body!!.string().parseJson()
        assertNotNull("Expected error field", json.get("error"))
        assertTrue(json.get("error").asString.contains("intervalMs"))
    }

    @Test
    fun `POST session start missing cameraId returns 400`() {
        val body = """{"intervalMs":5000,"wbMode":"auto"}"""
        val response = postJson("/api/session/start", body)
        assertEquals(400, response.code)
    }

    @Test
    fun `POST session start missing wbMode returns 400`() {
        val body = """{"intervalMs":5000,"cameraId":"main"}"""
        val response = postJson("/api/session/start", body)
        assertEquals(400, response.code)
    }

    // ─── POST /api/session/stop ───────────────────────────────────────────────────

    @Test
    fun `POST session stop returns ok=true`() {
        val response = postJson("/api/session/stop", "")
        assertEquals(200, response.code)

        val json = response.body!!.string().parseJson()
        assertTrue("Expected ok=true", json.get("ok").asBoolean)
    }

    // ─── GET /api/last-photo ──────────────────────────────────────────────────────

    @Test
    fun `GET last-photo returns 404 when no photo exists`() {
        // SessionManager was reset in @Before so lastJpegPath is null
        val response = get("/api/last-photo")
        assertEquals(404, response.code)
    }

    // ─── POST /api/settings ───────────────────────────────────────────────────────

    @Test
    fun `POST settings with ntfy topic saves and returns ok=true`() {
        val body = """{"ntfyTopic":"my-channel"}"""
        val response = postJson("/api/settings", body)
        assertEquals(200, response.code)

        val json = response.body!!.string().parseJson()
        assertTrue("Expected ok=true", json.get("ok").asBoolean)
    }

    @Test
    fun `POST settings missing ntfyTopic returns 400`() {
        val body = """{"something":"else"}"""
        val response = postJson("/api/settings", body)
        assertEquals(400, response.code)

        val json = response.body!!.string().parseJson()
        assertTrue(json.get("error").asString.contains("ntfyTopic"))
    }

    // ─── Unknown routes ───────────────────────────────────────────────────────────

    @Test
    fun `unknown route returns 404`() {
        val response = get("/api/does-not-exist")
        assertEquals(404, response.code)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private fun get(path: String): Response {
        val request = Request.Builder().url("$baseUrl$path").get().build()
        return client.newCall(request).execute()
    }

    private fun postJson(path: String, body: String): Response {
        val requestBody = body.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(requestBody)
            .build()
        return client.newCall(request).execute()
    }

    private fun String.parseJson(): JsonObject =
        JsonParser.parseString(this).asJsonObject
}
