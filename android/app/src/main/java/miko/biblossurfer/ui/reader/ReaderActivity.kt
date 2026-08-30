package miko.biblossurfer.ui.reader

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import miko.biblossurfer.BiblosSurferApp
import miko.biblossurfer.R
import miko.biblossurfer.data.DescriptiveError
import miko.biblossurfer.data.Errors
import miko.biblossurfer.data.OpenedPublication
import miko.biblossurfer.data.model.LibraryItem
import miko.biblossurfer.data.model.PublicationFormat
import miko.biblossurfer.data.tts.TtsController
import miko.biblossurfer.data.tts.TtsPlaybackState
import miko.biblossurfer.data.tts.TtsServiceDelegate
import miko.biblossurfer.ui.library.ErrorDismissing
import miko.biblossurfer.ui.subviews.ErrorView
import miko.biblossurfer.util.AccessibilityIdentifiers
import miko.biblossurfer.util.AnalyticsEvent
import miko.biblossurfer.util.AnalyticsTimer
import miko.biblossurfer.util.Debouncer
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.navigator.media.tts.AndroidTtsNavigatorFactory
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse
import org.json.JSONObject

class ReaderActivity : AppCompatActivity(), ReaderActions, TtsServiceDelegate, ErrorDismissing {

    companion object {
        private const val EXTRA_ITEM = "item"
        private const val NAVIGATOR_TAG = "navigator"

        fun intent(context: Context, item: LibraryItem): Intent =
            Intent(context, ReaderActivity::class.java).putExtra(EXTRA_ITEM, item)
    }

    private val app get() = application as BiblosSurferApp
    private lateinit var item: LibraryItem
    private var opened: OpenedPublication? = null
    private var viewModel: ReaderViewModel? = null
    private var tts: TtsController? = null
    private var session: ReaderSession? = null
    private val locationDebouncer = Debouncer(lifecycleScope)
    private val wordDebouncer = Debouncer(lifecycleScope)
    private var isMoving = false
    private var lastSelectionLocator: Locator? = null
    private var lastSpokenLocator: Locator? = null
    private var overlayState by mutableStateOf(OverlayUi())
    private var showTtsSettings by mutableStateOf(false)

    data class OverlayUi(
        val loading: Boolean = true,
        val error: DescriptiveError? = null,
        val title: String = "",
        val showTtsPanel: Boolean = false,
        val showSettings: Boolean = false,
    )

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        item = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_ITEM, LibraryItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ITEM)
        } ?: run {
            finish()
            return
        }

        supportFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        val overlay = findViewById<ComposeView>(R.id.reader_overlay)
        overlay.setContent {
            MaterialTheme {
                val vm = viewModel
                when {
                    overlayState.error != null -> ErrorView(
                        error = overlayState.error!!,
                        handler = this@ReaderActivity,
                    )
                    overlayState.loading -> Box(
                        Modifier.fillMaxSize().testTag(AccessibilityIdentifiers.Reader.container),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                    overlayState.showSettings && vm != null -> {
                        if (showTtsSettings) {
                            TTSSettingsView(viewModel = vm)
                        } else {
                            ReaderSettingsView(viewModel = vm, onOpenTts = { showTtsSettings = true })
                        }
                    }
                    else -> Scaffold(
                        modifier = Modifier.testTag(AccessibilityIdentifiers.Reader.container),
                        topBar = {
                            TopAppBar(
                                title = { Text(overlayState.title) },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            viewModel?.availableVoices = tts?.availableVoices.orEmpty()
                                            overlayState = overlayState.copy(showSettings = true)
                                            showTtsSettings = false
                                            viewModel?.openSettings()
                                        },
                                        modifier = Modifier.testTag(AccessibilityIdentifiers.Reader.settings),
                                    ) {
                                        Icon(Icons.Filled.TextFields, contentDescription = "Settings")
                                    }
                                }
                            )
                        }
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            if (overlayState.showTtsPanel && vm != null) {
                                TtsPanelView(
                                    viewModel = vm,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                )
                            }
                        }
                    }
                }
            }
        }

        val timer = AnalyticsTimer(AnalyticsEvent.openingPublication)
        timer.startTimer()
        lifecycleScope.launch {
            try {
                val result = app.serviceProvider.publicationOpener.open(item.fileURL)
                timer.endTimer()
                timer.reportToAnalytics()
                attachNavigator(result)
            } catch (error: Exception) {
                timer.endTimer()
                timer.reportToAnalytics()
                val descriptive = error as? DescriptiveError
                    ?: Errors.Publication.OpenFailed(item.title, error.localizedMessage ?: error.toString())
                overlayState = OverlayUi(loading = false, error = descriptive, title = item.title)
            }
        }
    }

    @OptIn(ExperimentalReadiumApi::class)
    private fun attachNavigator(opened: OpenedPublication) {
        this.opened = opened
        val initialLocation = item.locatorJSON?.let { Locator.fromJSON(JSONObject(it)) }
        val canSpeak = opened.format == PublicationFormat.EPUB &&
            AndroidTtsNavigatorFactory(application, opened.publication) != null
        val vm = ReaderViewModel(
            title = opened.title,
            format = opened.format,
            canSpeak = canSpeak,
            settings = app.serviceProvider.settings,
        )
        vm.actions = this
        viewModel = vm

        if (vm.viewProperties.value.canSpeak) {
            val controller = TtsController(
                application = application,
                publication = opened.publication,
                settings = app.serviceProvider.settings,
                bookTitle = opened.title,
                scope = lifecycleScope,
            )
            controller.delegate = this
            tts = controller
            vm.availableVoices = controller.availableVoices
        }

        when (opened.format) {
            PublicationFormat.EPUB -> {
                val factory = EpubNavigatorFactory(publication = opened.publication)
                supportFragmentManager.fragmentFactory = factory.createFragmentFactory(
                    initialLocator = initialLocation,
                    initialPreferences = app.serviceProvider.settings.epubPreferences(),
                    listener = epubListener,
                    configuration = EpubNavigatorFragment.Configuration(
                        selectionActionModeCallback = readFromHereCallback(),
                    ),
                )
            }
            PublicationFormat.PDF -> {
                val factory = PdfNavigatorFactory(
                    publication = opened.publication,
                    pdfEngineProvider = PdfiumEngineProvider(),
                )
                supportFragmentManager.fragmentFactory = factory.createFragmentFactory(
                    initialLocator = initialLocation,
                    initialPreferences = app.serviceProvider.settings.pdfPreferences(),
                    listener = pdfListener,
                )
            }
            PublicationFormat.UNKNOWN -> {
                overlayState = OverlayUi(
                    loading = false,
                    error = Errors.Publication.UnknownFormat(opened.title),
                    title = opened.title,
                )
                return
            }
        }

        val fragmentClass = when (opened.format) {
            PublicationFormat.PDF -> PdfNavigatorFragment::class.java
            else -> EpubNavigatorFragment::class.java
        }
        supportFragmentManager.commitNow {
            replace(R.id.navigator_container, fragmentClass, Bundle(), NAVIGATOR_TAG)
        }
        val navigator = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG)
        val epubNavigator = navigator as? EpubNavigatorFragment
        val pdfNavigator = navigator as? PdfNavigatorFragment<*, *>
        val visualNavigator = navigator as? VisualNavigator ?: return
        val preferences: ReaderPreferencesApplying = when {
            epubNavigator != null -> EpubPreferencesApplying(epubNavigator, lifecycleScope)
            pdfNavigator != null -> {
                @Suppress("UNCHECKED_CAST")
                PdfiumPreferencesApplying(
                    pdfNavigator as PdfNavigatorFragment<*, org.readium.adapter.pdfium.navigator.PdfiumPreferences>,
                    lifecycleScope,
                )
            }
            else -> return
        }
        session = ReaderSession(
            visualNavigator = visualNavigator,
            epubNavigator = epubNavigator,
            pdfNavigator = pdfNavigator,
            preferences = preferences,
        )
        observeLocator(navigator as Navigator)
        overlayState = OverlayUi(loading = false, title = opened.title)
    }

    private fun observeLocator(navigator: Navigator) {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                navigator.currentLocator.collect { locator -> persist(locator) }
            }
        }
    }

    private fun persist(locator: Locator) {
        locationDebouncer.schedule(1.0) {
            writeProgress(locator)
        }
    }

    private fun persistImmediately(locator: Locator) {
        locationDebouncer.cancel()
        writeProgress(locator)
    }

    private fun persistReadingPosition() {
        val locator = lastSpokenLocator
            ?: session?.visualNavigator?.currentLocator?.value
            ?: return
        persistImmediately(locator)
    }

    private fun writeProgress(locator: Locator) {
        val json = locator.toJSON().toString()
        app.serviceProvider.bookService.updateProgress(
            relativePath = item.fileURL.name,
            locatorJSON = json,
            progression = locator.locations.totalProgression ?: 0.0,
        )
    }

    private fun followSpokenRange(locator: Locator) {
        if (isMoving) return
        wordDebouncer.schedule(1.0) {
            isMoving = true
            lifecycleScope.launch {
                session?.visualNavigator?.go(locator, false)
                isMoving = false
            }
        }
    }

    private fun highlightUtterance(locator: Locator?) {
        val navigator = session?.epubNavigator as? DecorableNavigator ?: return
        val decorations = if (locator != null) {
            listOf(
                Decoration(
                    id = "tts-utterance",
                    locator = locator,
                    style = Decoration.Style.Highlight(tint = Color.parseColor("#FF9800")),
                )
            )
        } else {
            emptyList()
        }
        lifecycleScope.launch {
            navigator.applyDecorations(decorations, group = "tts")
        }
    }

    private fun readFromHereCallback() = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.add(0, R.id.read_from_here, 0, "Czytaj od tu")
            return true
        }
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (item.itemId == R.id.read_from_here) {
                readFromSelection()
                mode.finish()
                return true
            }
            return false
        }
        override fun onDestroyActionMode(mode: ActionMode) = Unit
    }

    @OptIn(ExperimentalReadiumApi::class)
    private fun readFromSelection() {
        val vm = viewModel ?: return
        if (!vm.viewProperties.value.canSpeak) return
        lifecycleScope.launch {
            val locator = session?.epubNavigator?.currentSelection()?.locator ?: lastSelectionLocator
            lastSelectionLocator = null
            overlayState = overlayState.copy(showTtsPanel = true)
            tts?.start(locator)
            session?.epubNavigator?.clearSelection()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            tts?.stop()
        }
        persistReadingPosition()
        locationDebouncer.flush()
        wordDebouncer.cancel()
    }

    override fun onDestroy() {
        tts?.close()
        super.onDestroy()
    }

    override fun playPauseTTS() {
        val controller = tts ?: return
        if (controller.state == TtsPlaybackState.STOPPED) {
            lifecycleScope.launch {
                val locator = session?.visualNavigator?.firstVisibleElementLocator()
                overlayState = overlayState.copy(showTtsPanel = true)
                controller.start(locator)
            }
        } else {
            controller.pauseOrResume()
        }
    }

    override fun stopTTS() {
        persistReadingPosition()
        tts?.stop()
        overlayState = overlayState.copy(showTtsPanel = false)
        highlightUtterance(null)
    }

    override fun nextUtterance() {
        tts?.next()
    }

    override fun previousUtterance() {
        tts?.previous()
    }

    override fun applyReaderSettings() {
        val settings = viewModel?.settings ?: return
        session?.preferences?.submitReaderPreferences(settings)
        tts?.applySettings()
    }

    override fun closeSettings() {
        overlayState = overlayState.copy(showSettings = false)
        showTtsSettings = false
    }

    override fun dismissPresentedError() {
        overlayState = overlayState.copy(error = null)
    }

    override fun dismissError() {
        finish()
    }

    override fun ttsServiceDidChange(isPlaying: Boolean, utteranceLocator: Locator?, tokenLocator: Locator?) {
        val state = when {
            !isPlaying && utteranceLocator == null -> TtsPlaybackState.STOPPED
            !isPlaying -> TtsPlaybackState.PAUSED
            else -> TtsPlaybackState.PLAYING
        }
        viewModel?.apply(state)
        overlayState = overlayState.copy(showTtsPanel = state != TtsPlaybackState.STOPPED)
        when (state) {
            TtsPlaybackState.STOPPED -> {
                persistReadingPosition()
                highlightUtterance(null)
            }
            TtsPlaybackState.PAUSED -> {
                lastSpokenLocator = utteranceLocator
                highlightUtterance(utteranceLocator)
            }
            TtsPlaybackState.PLAYING -> {
                lastSpokenLocator = tokenLocator ?: utteranceLocator
                highlightUtterance(utteranceLocator)
                tokenLocator?.let { followSpokenRange(it) }
            }
        }
    }

    override fun ttsServiceDidFail(error: DescriptiveError) {
        viewModel?.presentError(error)
        overlayState = overlayState.copy(error = error)
    }

    private val epubListener = object : EpubNavigatorFragment.Listener {
        override fun onExternalLinkActivated(url: org.readium.r2.shared.util.AbsoluteUrl) = Unit
    }
    private val pdfListener = object : PdfNavigatorFragment.Listener {}
}
