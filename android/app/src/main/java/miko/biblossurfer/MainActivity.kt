package miko.biblossurfer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import miko.biblossurfer.data.MockServiceProvider
import miko.biblossurfer.data.ServiceProvider
import miko.biblossurfer.data.model.LibraryItem
import miko.biblossurfer.ui.library.LibraryRouting
import miko.biblossurfer.ui.library.LibraryScreen
import miko.biblossurfer.ui.library.LibraryViewModel
import miko.biblossurfer.ui.reader.ReaderActivity
import miko.biblossurfer.util.UITestLaunchArgument
import miko.biblossurfer.util.isUITestStubLaunch
import java.io.File

class MainActivity : ComponentActivity(), LibraryRouting {
    private lateinit var libraryViewModel: LibraryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as BiblosSurferApp
        val stub = intent.extras.isUITestStubLaunch() ||
            intent.getBooleanExtra(UITestLaunchArgument.stub, false)
        if (stub) {
            app.useStubLibrary(true)
        }
        val provider = app.serviceProvider
        libraryViewModel = LibraryViewModel(provider.libraryService).also { it.router = this }

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    LibraryScreen(viewModel = libraryViewModel)
                }
            }
        }

        handleIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    override fun openBook(item: LibraryItem) {
        startActivity(ReaderActivity.intent(this, item))
    }

    private fun handleIncoming(intent: Intent) {
        val uri: Uri = intent.data ?: return
        lifecycleScope.launch {
            val copied = withContext(Dispatchers.IO) {
                copyIncoming(uri)
            } ?: return@launch
            runCatching { libraryViewModel.importBook(copied) }
        }
    }

    private fun copyIncoming(uri: Uri): File? {
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: return null
        val dest = File(cacheDir, name)
        contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        return dest
    }
}
