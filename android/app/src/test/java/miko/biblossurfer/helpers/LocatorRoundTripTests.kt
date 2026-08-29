package miko.biblossurfer.helpers

import androidx.test.ext.junit.runners.AndroidJUnit4
import miko.biblossurfer.support.TestFixtures
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.annotation.Config

/**
 * The reading position is stored as Locator JSON, so a broken round trip means every reader
 * silently reopens at page one. These tests also pin the wire format, which has to stay readable by
 * the Swift toolkit.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class LocatorRoundTripTests {
    @Test
    fun locatorSurvivesJSONRoundTrip() {
        val original = TestFixtures.locator()
        val json = original.toJSON()
        val restored = Locator.fromJSON(json)!!

        assertEquals(original.href, restored.href)
        assertEquals(original.mediaType, restored.mediaType)
        assertEquals(original.title, restored.title)
        assertEquals(original.locations.progression, restored.locations.progression)
        assertEquals(original.locations.totalProgression, restored.locations.totalProgression)
        assertEquals(original.locations.position, restored.locations.position)
        assertEquals(original.text.highlight, restored.text.highlight)
    }

    @Test
    fun jsonUsesTheKeysSharedWithTheSwiftToolkit() {
        val json = TestFixtures.locator().toJSON()
        assertNotNull(json.opt("href"))
        assertEquals(MediaType.XHTML.toString(), json.optString("type"))
        assertNotNull(json.optJSONObject("locations"))
        assertNotNull(json.optJSONObject("text"))
    }

    @Test
    fun minimalLocatorRoundTripsWithoutOptionalFields() {
        val original = TestFixtures.locator(
            title = null,
            progression = null,
            totalProgression = null,
            position = null,
            highlight = null,
        )
        val restored = Locator.fromJSON(original.toJSON())!!
        assertEquals(original.href, restored.href)
        assertNull(restored.title)
        assertNull(restored.locations.progression)
        assertNull(restored.locations.position)
    }
}
