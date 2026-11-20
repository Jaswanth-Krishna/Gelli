package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FirebaseRtdbProvider
import com.example.data.GuardPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FlashGet Guard", appName)
    }

    @Test
    fun `guard prefs stores and reads 6 digit pairing code`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        GuardPrefs.clearPairingCode(context)
        assertNull(GuardPrefs.getPairingCode(context))

        GuardPrefs.savePairingCode(context, "482910")
        assertEquals("482910", GuardPrefs.getPairingCode(context))
        assertEquals(
            "482910",
            context.getSharedPreferences("flashget_prefs", Context.MODE_PRIVATE)
                .getString("pairing_code", null)
        )
    }

    @Test
    fun `database url matches configured rtdb endpoint`() {
        assertEquals(
            "https://i-w-flashget-parental-control-default-rtdb.firebaseio.com/",
            FirebaseRtdbProvider.DATABASE_URL
        )
    }
}
