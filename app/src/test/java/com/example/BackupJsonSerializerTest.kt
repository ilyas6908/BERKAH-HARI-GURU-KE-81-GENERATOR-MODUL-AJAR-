package com.example

import com.example.data.backup.BackupJsonSerializer
import com.example.data.model.DocType
import com.example.data.model.TeacherProfile
import com.example.data.model.TeachingDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupJsonSerializerTest {

    @Test
    fun serializeAndDeserialize_roundTripPreservesProfileAndDocs() {
        val profile = TeacherProfile.sampleDefault("uid_1")
        val docs = listOf(
            TeachingDocument(
                id = "doc_1",
                userId = "uid_1",
                docType = DocType.MODUL_AJAR.name,
                title = "Modul Ajar Ekosistem",
                educationLevel = "SD/MI",
                phase = "Fase C (Kelas 5-6)",
                grade = "Kelas 5 / Semester 1",
                subject = "IPAS",
                topic = "Rantai Makanan",
                duration = "2 x 35 Menit",
                pedagogicalModel = "Project-Based Learning (PjBL)",
                deepLearningPrinciples = listOf("Berkesadaran (Mindful)", "Bermakna (Meaningful)"),
                graduateDimensions = listOf("Penalaran Kritis", "Kolaborasi"),
                contentMarkdown = "# Modul Ajar\nKonten lengkap",
                starred = true
            )
        )

        val json = BackupJsonSerializer.serialize(profile, docs)
        val parsed = BackupJsonSerializer.deserialize(json)

        assertTrue(parsed.isSuccess)
        val bundle = parsed.getOrThrow()
        assertEquals(TeacherProfile.DEFAULT_TEACHER_NAME, bundle.profile?.teacherName)
        assertEquals(TeacherProfile.DEFAULT_TEACHER_NIP, bundle.profile?.nip)
        assertEquals(TeacherProfile.DEFAULT_SCHOOL_NAME, bundle.profile?.schoolName)
        assertEquals(TeacherProfile.DEFAULT_SCHOOL_NPSN, bundle.profile?.schoolNpsn)
        assertEquals(TeacherProfile.DEFAULT_SCHOOL_ADDRESS, bundle.profile?.schoolAddress)
        assertEquals(TeacherProfile.DEFAULT_PRINCIPAL_NAME, bundle.profile?.principalName)
        assertEquals(TeacherProfile.DEFAULT_PRINCIPAL_NIP, bundle.profile?.principalNip)
        assertEquals(1, bundle.documents.size)
        assertEquals("Modul Ajar Ekosistem", bundle.documents.first().title)
        assertEquals(true, bundle.documents.first().starred)
    }

    @Test
    fun deserialize_invalidJsonReturnsFailure() {
        val parsed = BackupJsonSerializer.deserialize("{not valid json")
        assertTrue(parsed.isFailure)
    }
}
