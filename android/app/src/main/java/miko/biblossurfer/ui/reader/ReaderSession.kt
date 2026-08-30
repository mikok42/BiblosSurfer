package miko.biblossurfer.ui.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import miko.biblossurfer.data.ReaderSettingsStore
import org.readium.adapter.pdfium.navigator.PdfiumPreferences
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.pdf.PdfNavigatorFragment

fun interface ReaderPreferencesApplying {
    fun submitReaderPreferences(settings: ReaderSettingsStore)
}

class EpubPreferencesApplying(
    private val navigator: EpubNavigatorFragment,
    private val scope: CoroutineScope,
) : ReaderPreferencesApplying {
    override fun submitReaderPreferences(settings: ReaderSettingsStore) {
        scope.launch {
            navigator.submitPreferences(settings.epubPreferences())
        }
    }
}

class PdfiumPreferencesApplying(
    private val navigator: PdfNavigatorFragment<*, PdfiumPreferences>,
    private val scope: CoroutineScope,
) : ReaderPreferencesApplying {
    override fun submitReaderPreferences(settings: ReaderSettingsStore) {
        scope.launch {
            navigator.submitPreferences(settings.pdfPreferences())
        }
    }
}

data class ReaderSession(
    val visualNavigator: VisualNavigator,
    val epubNavigator: EpubNavigatorFragment?,
    val pdfNavigator: PdfNavigatorFragment<*, *>?,
    val preferences: ReaderPreferencesApplying,
)
