package miko.biblossurfer.helpers

import androidx.test.ext.junit.runners.AndroidJUnit4
import miko.biblossurfer.support.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SampleBookBundleTests {
    @Test
    fun sampleBookIsBundled() {
        val file = TestFixtures.sampleBookFile!!
        assertTrue(file.exists())
    }

    @Test
    fun sampleBookLooksLikeAnEPUBZip() {
        val file = TestFixtures.sampleBookFile!!
        val data = file.readBytes()
        assertTrue(data.isNotEmpty())
        assertEquals(listOf('P'.code.toByte(), 'K'.code.toByte()), data.take(2))
    }

    @Test
    fun gutenbergHistoryIndexIsBundled() {
        val file = TestFixtures.gutenbergHistoryFile!!
        assertTrue(file.exists())
        val data = file.readBytes()
        assertEquals(listOf('P'.code.toByte(), 'K'.code.toByte()), data.take(2))
    }

    @Test
    fun samplePDFIsBundled() {
        val file = TestFixtures.samplePdfFile!!
        assertTrue(file.exists())
        val data = file.readBytes()
        assertEquals("%PDF-".toByteArray().toList(), data.take(5))
    }

    @Test
    fun temporaryCopyIsIndependentOfTheBundle() {
        val copy = TestFixtures.temporarySampleBookFile()
        try {
            assertTrue(copy.exists())
            assertNotEquals(copy.absolutePath, TestFixtures.sampleBookFile?.absolutePath)
        } finally {
            copy.parentFile?.deleteRecursively()
        }
    }
}
