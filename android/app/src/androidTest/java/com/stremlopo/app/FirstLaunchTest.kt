package com.stremlopo.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FirstLaunchTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun launchShowsMoviesWithoutServerOrManualCoverSetup() {
        compose.onNodeWithText("STREMLOPO").assertIsDisplayed()
        compose.onNodeWithText("Assistir",substring=false).assertIsDisplayed()
        compose.onNodeWithText("Servidor HTTPS").assertDoesNotExist()
        compose.waitForIdle()
        Thread.sleep(1000) // Give asset artwork a frame before the review screenshot.
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val bitmap=instrumentation.uiAutomation.takeScreenshot()
        val file=File(compose.activity.getExternalFilesDir(null),"stremlopo-home.png")
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        compose.onNodeWithText("Filmes",substring=false).performClick()
        compose.onNodeWithText("4 filmes disponíveis").assertIsDisplayed()
        compose.onNodeWithText("Sintel",substring=false).assertIsDisplayed()
        compose.onNodeWithText("Big Buck Bunny",substring=false).assertIsDisplayed()
        val allFilms=instrumentation.uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),"stremlopo-filmes.png").outputStream().use {allFilms.compress(Bitmap.CompressFormat.PNG,100,it)}
        compose.onNodeWithText("Big Buck Bunny",substring=false).performClick()
        compose.onNodeWithText("Assistir agora").performClick()
        compose.waitUntil(timeoutMillis=60000) {
            compose.onAllNodesWithContentDescription("Vídeo em reprodução").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Fechar reprodução").performClick()
    }
}
