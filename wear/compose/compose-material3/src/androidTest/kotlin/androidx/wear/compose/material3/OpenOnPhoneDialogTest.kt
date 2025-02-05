/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.compose.material3

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class OpenOnPhoneDialogTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun openOnPhone_supports_testtag() {
        rule.setContentWithTheme {
            val style = OpenOnPhoneDialogDefaults.curvedTextStyle
            OpenOnPhoneDialog(
                visible = true,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
                curvedText = { openOnPhoneDialogCurvedText(text = CurvedText, style = style) }
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun openOnPhone_supports_swipeToDismiss() {
        var dismissCounter = 0
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(true) }
            val style = OpenOnPhoneDialogDefaults.curvedTextStyle
            OpenOnPhoneDialog(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                },
                visible = visible,
                curvedText = { openOnPhoneDialogCurvedText(text = CurvedText, style = style) }
            )
        }
        rule.mainClock.advanceTimeBy(OpenOnPhoneDialogDefaults.DurationMillis / 2)
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        // Advancing time so that the dialog is dismissed
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @Test
    fun hides_openOnPhone_when_show_false() {
        rule.setContentWithTheme {
            val style = OpenOnPhoneDialogDefaults.curvedTextStyle
            OpenOnPhoneDialog(
                visible = false,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
                curvedText = { openOnPhoneDialogCurvedText(text = CurvedText, style = style) }
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun openOnPhone_displays_icon() {
        rule.setContentWithTheme {
            val style = OpenOnPhoneDialogDefaults.curvedTextStyle
            OpenOnPhoneDialog(
                onDismissRequest = {},
                visible = true,
                curvedText = { openOnPhoneDialogCurvedText(text = CurvedText, style = style) }
            ) {
                TestImage(IconTestTag)
            }
        }
        rule.onNodeWithTag(IconTestTag).assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun openOnPhone_onDismissRequest_not_called_when_hidden() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            val style = OpenOnPhoneDialogDefaults.curvedTextStyle
            OpenOnPhoneDialog(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = { dismissCounter++ },
                durationMillis = 1000,
                visible = visible.value,
                curvedText = { openOnPhoneDialogCurvedText(text = CurvedText, style = style) }
            )
        }
        rule.waitForIdle()
        // First we have to wait until animation completes and goes into idle state.
        // onDismissRequest will be called once it's finished - so dismissCounter will be 1.
        Assert.assertEquals(1, dismissCounter)
        visible.value = false
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))

        // However, onDismissRequest should not be called when show.value becomes false and dialog
        // is hidden. That's why it should remain as 1.
        Assert.assertEquals(1, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun openOnPhone_calls_onDismissRequest_on_timeout() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            val style = OpenOnPhoneDialogDefaults.curvedTextStyle
            OpenOnPhoneDialog(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    dismissCounter++
                    visible.value = false
                },
                durationMillis = 100,
                visible = visible.value,
                curvedText = { openOnPhoneDialogCurvedText(text = CurvedText, style = style) }
            )
        }
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(1, dismissCounter)
    }

    @Test
    fun openOnPhone_dismissed_after_timeout() {
        var dismissed = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            val style = OpenOnPhoneDialogDefaults.curvedTextStyle
            OpenOnPhoneDialog(
                onDismissRequest = { dismissed = true },
                visible = true,
                curvedText = { openOnPhoneDialogCurvedText(text = CurvedText, style = style) }
            ) {}
        }
        // Timeout longer than default confirmation duration
        rule.mainClock.advanceTimeBy(OpenOnPhoneDialogDefaults.DurationMillis + 1000)
        assert(dismissed)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun openOnPhone_correct_colors() {
        rule.mainClock.autoAdvance = false
        var expectedIconColor: Color = Color.Unspecified
        var expectedIconContainerColor: Color = Color.Unspecified
        var expectedProgressIndicatorColor: Color = Color.Unspecified
        var expectedProgressTrackColor: Color = Color.Unspecified
        rule.setContentWithTheme {
            val style = OpenOnPhoneDialogDefaults.curvedTextStyle
            OpenOnPhoneDialog(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                visible = true,
                curvedText = { openOnPhoneDialogCurvedText(text = CurvedText, style = style) }
            )
            expectedIconColor = MaterialTheme.colorScheme.primary
            expectedIconContainerColor = MaterialTheme.colorScheme.primaryContainer
            expectedProgressIndicatorColor = MaterialTheme.colorScheme.primary
            expectedProgressTrackColor = MaterialTheme.colorScheme.onPrimary
        }
        // Advance time by half of the default confirmation duration, so that the track and
        // indicator are shown
        rule.mainClock.advanceTimeBy(OpenOnPhoneDialogDefaults.DurationMillis / 2)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedIconColor)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedIconContainerColor)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedProgressIndicatorColor)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedProgressTrackColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun openOnPhone_custom_colors() {
        rule.mainClock.autoAdvance = false
        val customIconColor: Color = Color.Red
        val customIconContainerColor: Color = Color.Green
        val customProgressIndicatorColor: Color = Color.Blue
        val customProgressTrackColor: Color = Color.Magenta
        rule.setContentWithTheme {
            val style = OpenOnPhoneDialogDefaults.curvedTextStyle
            OpenOnPhoneDialog(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    OpenOnPhoneDialogDefaults.colors(
                        iconColor = customIconColor,
                        iconContainerColor = customIconContainerColor,
                        progressIndicatorColor = customProgressIndicatorColor,
                        progressTrackColor = customProgressTrackColor
                    ),
                visible = true,
                curvedText = { openOnPhoneDialogCurvedText(text = CurvedText, style = style) }
            )
        }
        // Advance time by half of the default confirmation duration, so that the track and
        // indicator are shown
        rule.mainClock.advanceTimeBy(OpenOnPhoneDialogDefaults.DurationMillis / 2)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconColor)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconContainerColor)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(customProgressIndicatorColor)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customProgressTrackColor)
    }
}

private const val IconTestTag = "icon"
private const val CurvedText = "CurvedText"
