package miko.biblossurfer.data

/**
 * Errors that can be shown in [miko.biblossurfer.ui.subviews.ErrorView].
 * Messages match the iOS `Errors.swift` copy so both platforms surface the same text.
 */
sealed class DescriptiveError : Exception() {
    abstract val title: String
    abstract val description: String
    override val message: String get() = description
}

object Errors {
    sealed class Library : DescriptiveError() {
        data class UnsupportedFormat(val fileExtension: String) : Library() {
            override val title: String = "Unsupported format"
            override val description: String =
                "[Library] BiblosSurfer cannot open .$fileExtension files. Try EPUB or PDF."
        }

        data class CopyFailed(val fileName: String, val underlying: String) : Library() {
            override val title: String = "Could not add the book"
            override val description: String =
                "[Library] Could not add $fileName to your library: $underlying"
        }

        data class FileMissing(val fileName: String) : Library() {
            override val title: String = "Book file missing"
            override val description: String =
                "[Library] $fileName is no longer on this device."
        }
    }

    sealed class Publication : DescriptiveError() {
        data class OpenFailed(val bookTitle: String, val underlying: String) : Publication() {
            override val title: String = "Could not open the book"
            override val description: String =
                "[Publication] Could not open $bookTitle: $underlying"
        }

        data class UnsupportedForReading(val bookTitle: String) : Publication() {
            override val title: String = "Cannot display this book"
            override val description: String =
                "[Publication] $bookTitle parsed, but its format cannot be displayed."
        }

        data class UnknownFormat(val bookTitle: String) : Publication() {
            override val title: String = "Unknown format"
            override val description: String =
                "[Publication] $bookTitle has an unknown format."
        }

        data class NoNavigator(val bookTitle: String) : Publication() {
            override val title: String = "Cannot display this book"
            override val description: String =
                "[Publication] No reader is available for the format of $bookTitle."
        }
    }

    sealed class Tts : DescriptiveError() {
        data object NoSpeakableContent : Tts() {
            override val title: String = "Nothing to read aloud"
            override val description: String =
                "[TTS] This book has no text that can be read aloud."
        }

        data class NoVoiceForLanguage(val language: String) : Tts() {
            override val title: String = "Missing voice"
            override val description: String =
                "[TTS] No installed voice speaks $language. Add one in Settings, Accessibility, Spoken Content."
        }

        data class EngineFailed(val underlying: String) : Tts() {
            override val title: String = "Read aloud stopped"
            override val description: String =
                "[TTS] Speech synthesis failed: $underlying"
        }
    }
}
