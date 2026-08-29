package miko.biblossurfer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import miko.biblossurfer.data.UITestStubLibraryService
import miko.biblossurfer.util.AccessibilityIdentifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchUiTests {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches() {
        composeRule.onNodeWithTag(AccessibilityIdentifiers.Library.title).assertIsDisplayed()
    }

    @Test
    fun openingSampleBookShowsItsText() {
        val cell = AccessibilityIdentifiers.Library.cell(UITestStubLibraryService.sampleTitle)
        composeRule.onNodeWithTag(cell).assertIsDisplayed()
        composeRule.onNodeWithTag(cell).performClick()
        composeRule.onNodeWithTag(AccessibilityIdentifiers.Reader.container).assertIsDisplayed()
    }
}
