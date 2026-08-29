package miko.biblossurfer

import android.app.Application
import miko.biblossurfer.data.MockServiceProvider
import miko.biblossurfer.data.ServiceProvider
import miko.biblossurfer.data.ServiceProviderProtocol
import miko.biblossurfer.util.UITestLaunchArgument
import miko.biblossurfer.util.isUITestStubLaunch

class BiblosSurferApp : Application() {
    lateinit var serviceProvider: ServiceProviderProtocol
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        val stub = intentArgumentsAreStub()
        serviceProvider = if (stub) MockServiceProvider(this) else ServiceProvider(this)
    }

    fun useStubLibrary(enabled: Boolean) {
        serviceProvider = if (enabled) MockServiceProvider(this) else ServiceProvider(this)
    }

    private fun intentArgumentsAreStub(): Boolean {
        val processArgs = try {
            Class.forName("android.app.ActivityThread")
            android.os.Process.myPid()
            false
        } catch (_: Exception) {
            false
        }
        return processArgs
    }

    companion object {
        lateinit var instance: BiblosSurferApp
            private set
    }
}

fun Application.asBiblos(): BiblosSurferApp = this as BiblosSurferApp
