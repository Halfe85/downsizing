package net.utgardnet.downsizing

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import net.utgardnet.downsizing.data.SetupDetails
import net.utgardnet.downsizing.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun requiredStepsValidateRetainValuesAndCompleteInOrder() {
        var result: SetupDetails? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent { DownsizingTheme { OnboardingFlow(false, null) { result = it } } }
        compose.onNodeWithText("Next").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Flag of Norway").assertExists()
        compose.onNodeWithText("Norway").performClick()
        next()
        compose.onNodeWithText("Next").assertIsNotEnabled()
        compose.onNodeWithText("Name").performTextInput("Kari")
        next()
        compose.onNodeWithText("Date of birth").performTextInput("31.02.1990")
        compose.onNodeWithText("Next").assertIsNotEnabled()
        compose.onNodeWithText("Date of birth").performTextReplacement("24.06.1990")
        next()
        compose.onNodeWithText("Height (cm)").performTextInput("172,5")
        compose.onNodeWithText("Weight (kg)").performTextInput("82,5")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("172,5").assertExists()
        next()
        compose.onNodeWithText("Weight goal (kg)").performTextInput("0")
        compose.onNodeWithText("Next").assertIsNotEnabled()
        compose.onNodeWithText("Weight goal (kg)").performTextReplacement("75")
        compose.onNodeWithText("Back").performScrollTo().performClick()
        compose.onNodeWithText("82,5").assertExists()
        next()
        next()
        compose.runOnIdle {
            assertEquals("NO", result!!.profile.country)
            assertEquals("Kari", result!!.profile.name)
            assertEquals(172.5, result!!.profile.heightCm, 0.0)
            assertEquals(82.5, result!!.weightKg, 0.0)
            assertEquals(75.0, result!!.goalKg, 0.0)
        }
    }

    @Test fun themeFollowsSystemConfiguration() {
        var light: Color? = null
        var dark: Color? = null
        compose.setContent {
            val base = LocalConfiguration.current
            CompositionLocalProvider(LocalConfiguration provides Configuration(base).apply {
                uiMode = Configuration.UI_MODE_NIGHT_NO
            }) { DownsizingTheme { light = MaterialTheme.colorScheme.background } }
            CompositionLocalProvider(LocalConfiguration provides Configuration(base).apply {
                uiMode = Configuration.UI_MODE_NIGHT_YES
            }) { DownsizingTheme { dark = MaterialTheme.colorScheme.background } }
        }
        compose.runOnIdle {
            assertEquals(Color.White, light)
            assertEquals(Color(0xFF1B1F20), dark)
        }
    }

    private fun next() { compose.onNodeWithText("Next").performScrollTo().performClick() }
}
