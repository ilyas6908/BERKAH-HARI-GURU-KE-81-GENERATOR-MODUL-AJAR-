package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.model.DocType
import com.example.data.model.TeacherProfile
import com.example.data.model.TeachingDocument
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.theme.AjarMerdekaTheme
import com.example.ui.viewmodel.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeDashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `dashboard renders profile summary, school identity, and triggers quick access module clicks`() {
        var selectedDocType: DocType? = null
        var openedGenerator = false
        var openedCollection = false
        var openedBackup = false

        val profile = TeacherProfile.sampleDefault(userId = "u1")
        val sampleDocs = listOf(
            TeachingDocument(
                id = "doc-1",
                userId = "u1",
                docType = DocType.MODUL_AJAR.name,
                title = "Modul Ajar Ekosistem",
                subject = "IPA",
                grade = "Kelas 7 / Semester 1",
                starred = true
            )
        )

        composeTestRule.setContent {
            AjarMerdekaTheme {
                HomeDashboardScreen(
                    docsState = UiState.Success(sampleDocs),
                    profileState = UiState.Success(profile),
                    cloudBackupsState = UiState.Success(emptyList()),
                    localBackups = emptyList(),
                    userEmail = "ilyas@sekolah.id",
                    userName = "Ilyas",
                    onSelectMenuForGeneration = { selectedDocType = it },
                    onNavigateToGenerator = { openedGenerator = true },
                    onNavigateToCollection = { openedCollection = true },
                    onNavigateToBackup = { openedBackup = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ilyas, S.Pd.").assertIsDisplayed()
        composeTestRule.onNodeWithText("NIP. 196908052007011026").assertIsDisplayed()
        composeTestRule.onNodeWithText("SMPN 1 GONDANG KAB. MOJOKERTO (NPSN: 20502646)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jalan Raya Karangkuten, Kec. Gondang, Kab. Mojokerto, 61372").assertIsDisplayed()

        composeTestRule.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag("quick_action_generator"))
        composeTestRule.onNodeWithTag("quick_action_generator").performClick()
        assertTrue(openedGenerator)

        composeTestRule.onNodeWithTag("quick_action_collection").performClick()
        assertTrue(openedCollection)

        composeTestRule.onNodeWithTag("quick_action_backup").performClick()
        assertTrue(openedBackup)

        composeTestRule.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag("menu_card_modul_ajar"))
        composeTestRule.onNodeWithTag("menu_card_modul_ajar").performClick()
        assertEquals(DocType.MODUL_AJAR, selectedDocType)

        composeTestRule.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag("menu_card_lkpd"))
        composeTestRule.onNodeWithTag("menu_card_lkpd").performClick()
        assertEquals(DocType.LKPD, selectedDocType)
    }
}
