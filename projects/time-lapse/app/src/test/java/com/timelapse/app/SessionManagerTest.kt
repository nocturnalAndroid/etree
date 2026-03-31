package com.timelapse.app

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Unit tests for SessionManager's pure-state behaviour.
 *
 * SessionManager is a Kotlin singleton, so state bleeds across tests.
 * Each test begins with a @Before that resets the observable state via the
 * public API surface.  We deliberately skip startSession / startForegroundService
 * paths because those require a live Android Service.
 *
 * NOTE: SessionManager.stopSession(context) calls context.startService — that
 * must resolve cleanly from a mock Context, which we provide.
 */
class SessionManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun resetState() {
        // Minimal SharedPreferences stub so preference reads/writes don't crash.
        mockEditor = mock {
            on { putString(any(), any()) } doReturn mock
            on { apply() } doAnswer { Unit }
        }
        mockPrefs = mock {
            on { getString(eq(App.PREF_NTFY_TOPIC), any()) } doReturn ""
            on { edit() } doReturn mockEditor
        }
        mockContext = mock {
            on { getSharedPreferences(any(), any()) } doReturn mockPrefs
            on { packageName } doReturn "com.timelapse.app"
            on { startService(any()) } doReturn null
            on { applicationContext } doReturn mock()
        }

        // Drive the singleton back to a known baseline without touching Android
        // internals: stopSession sets isRunning=false; the remaining atomics we
        // reset through public setters / by reading them via getStatus().
        SessionManager.stopSession(mockContext)
        SessionManager.updateNextCaptureMs(0L)
        SessionManager.setFolderId("__reset__")   // non-null so later assertions are clean
        // Clear folderId back to null by calling stopSession which doesn't reset it,
        // so we re-expose it through getFolderId for targeted tests only.
    }

    // ─── Initial / idle state ──────────────────────────────────────────────────

    @Test
    fun `initial state after reset is not running`() {
        val status = SessionManager.getStatus()
        assertFalse("Should not be running after reset", status.isRunning)
    }

    @Test
    fun `getStatus returns consistent snapshot`() {
        val s1 = SessionManager.getStatus()
        val s2 = SessionManager.getStatus()
        // Two consecutive calls return the same running/count values (no race between them)
        assertEquals(s1.isRunning, s2.isRunning)
        assertEquals(s1.photoCount, s2.photoCount)
    }

    // ─── onPhotoCaptured ───────────────────────────────────────────────────────

    @Test
    fun `onPhotoCaptured increments photoCount`() {
        // Verify count starts at a known value by reading it
        val before = SessionManager.getStatus().photoCount

        val dng = File.createTempFile("shot", ".dng")
        val jpeg = File.createTempFile("shot", ".jpg")
        try {
            SessionManager.onPhotoCaptured(mockContext, dng, jpeg)
            assertEquals(before + 1, SessionManager.getStatus().photoCount)
        } finally {
            dng.delete()
            jpeg.delete()
        }
    }

    @Test
    fun `onPhotoCaptured updates lastJpegPath`() {
        val dng = File.createTempFile("shot", ".dng")
        val jpeg = File.createTempFile("shot", ".jpg")
        try {
            SessionManager.onPhotoCaptured(mockContext, dng, jpeg)
            // getLastJpegFile() only returns a File if the path on disk exists; the
            // temp files do exist at this point so we get a non-null result.
            val last = SessionManager.getLastJpegFile()
            assertEquals(jpeg.absolutePath, last?.absolutePath)
        } finally {
            dng.delete()
            jpeg.delete()
        }
    }

    // ─── setFolderId / getFolderId ─────────────────────────────────────────────

    @Test
    fun `setFolderId and getFolderId round-trip`() {
        SessionManager.setFolderId("folder-abc-123")
        assertEquals("folder-abc-123", SessionManager.getFolderId())
    }

    // ─── updateNextCaptureMs ───────────────────────────────────────────────────

    @Test
    fun `updateNextCaptureMs is reflected in getStatus`() {
        val ts = 9_999_999_999L
        SessionManager.updateNextCaptureMs(ts)
        assertEquals(ts, SessionManager.getStatus().nextCaptureMs)
    }

    // ─── Thread safety ─────────────────────────────────────────────────────────

    /**
     * Fire 10 concurrent onPhotoCaptured calls and assert all increments land.
     *
     * photoCount is an AtomicInteger so this should always pass; if it were a
     * plain Int the test would be flaky.  Having the test here documents and
     * guards that property.
     *
     * NOTE: We clear out temp files in a finally block; if onPhotoCaptured ever
     * changes to do async work the latch gives it 5 s to finish.
     */
    @Test
    fun `photoCount is thread-safe under concurrent onPhotoCaptured calls`() {
        // Count baseline (carry-over from earlier test methods in this run)
        val baseline = SessionManager.getStatus().photoCount

        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val tempFiles = mutableListOf<File>()

        repeat(threadCount) {
            executor.submit {
                try {
                    val dng = File.createTempFile("par_shot", ".dng")
                    val jpeg = File.createTempFile("par_shot", ".jpg")
                    synchronized(tempFiles) {
                        tempFiles += dng
                        tempFiles += jpeg
                    }
                    SessionManager.onPhotoCaptured(mockContext, dng, jpeg)
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("Timed out waiting for concurrent captures", latch.await(5, TimeUnit.SECONDS))
        executor.shutdown()

        assertEquals(
            "Expected $threadCount increments from concurrent calls",
            baseline + threadCount,
            SessionManager.getStatus().photoCount
        )

        // Clean up temp files
        synchronized(tempFiles) { tempFiles.forEach { it.delete() } }
    }

    // ─── Preferences ──────────────────────────────────────────────────────────

    /**
     * setNtfyTopic / getNtfyTopic delegate to SharedPreferences.
     * We verify the mock is called correctly rather than checking storage
     * (there is no real storage in a JVM unit test).
     */
    @Test
    fun `setNtfyTopic and getNtfyTopic round-trip through SharedPreferences`() {
        // Arrange: make the mock return what we intend to "save"
        val savedValues = mutableMapOf<String, String>()
        val captureEditor = mock<SharedPreferences.Editor> {
            on { putString(any(), any()) } doAnswer { inv ->
                savedValues[inv.getArgument(0)] = inv.getArgument(1)
                mock
            }
            on { apply() } doAnswer { Unit }
        }
        val capturePrefs = mock<SharedPreferences> {
            on { getString(eq(App.PREF_NTFY_TOPIC), any()) } doAnswer {
                savedValues[App.PREF_NTFY_TOPIC] ?: ""
            }
            on { edit() } doReturn captureEditor
        }
        val ctx = mock<Context> {
            on { getSharedPreferences(any(), any()) } doReturn capturePrefs
        }

        SessionManager.setNtfyTopic(ctx, "my-channel")
        assertEquals("my-channel", SessionManager.getNtfyTopic(ctx))
    }
}
