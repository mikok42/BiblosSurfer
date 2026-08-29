package miko.biblossurfer.util

object UITestLaunchArgument {
    const val stub = "UITestStub"
}

fun Array<String>.isUITestStubLaunch(): Boolean = contains(UITestLaunchArgument.stub)

fun android.os.Bundle?.isUITestStubLaunch(): Boolean =
    this?.getBoolean(UITestLaunchArgument.stub, false) == true ||
        this?.getString(UITestLaunchArgument.stub) != null
