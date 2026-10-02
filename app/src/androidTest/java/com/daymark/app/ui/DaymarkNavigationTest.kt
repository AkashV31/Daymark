package com.daymark.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daymark.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaymarkNavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun homeAndPrimaryNavigationAreAvailable() {
        composeRule.onNodeWithText("DAYMARK").assertIsDisplayed()
        composeRule.onAllNodesWithText("Schedule").onFirst().performClick()
        composeRule.onAllNodesWithText("Notes").onFirst().performClick()
        composeRule.onNodeWithText("Capture an idea before it disappears.").assertIsDisplayed()
    }

    @Test fun moreHubGroupingAndBackNavigation() {
        composeRule.onAllNodesWithText("More").onFirst().performClick()
        composeRule.onNodeWithText("PLAN").assertIsDisplayed()
        composeRule.onNodeWithText("Goals").performClick()
        composeRule.onNodeWithText("Goals in motion").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText("PLAN").assertIsDisplayed()
    }
}
