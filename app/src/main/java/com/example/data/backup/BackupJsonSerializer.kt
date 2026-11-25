package com.example.data.backup

import com.example.data.model.DocType
import com.example.data.model.TeacherProfile
import com.example.data.model.TeachingDocument
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupBundle(
    val version: Int = 1,
    val exportedAt: String = "",
    val attribution: String = TeacherProfile.POWERED_BY_ATTRIBUTION,
    val profile: TeacherProfile? = null,
    val documents: List<TeachingDocument> = emptyList()
)

object BackupJsonSerializer {
    private const val CURRENT_VERSION = 1

    fun serialize(
        profile: TeacherProfile?,
        documents: List<TeachingDocument>
    ): String {
        val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val root = JSONObject().apply {
            put("app", "AjarMerdeka")
            put("version", CURRENT_VERSION)
            put("exportedAt", timestampFormat.format(Date()))
            put("attribution", TeacherProfile.POWERED_BY_ATTRIBUTION)
            if (profile != null) {
                put("profile", JSONObject().apply {
                    put("teacherName", profile.teacherName)
                    put("nip", profile.nip)
                    put("schoolName", profile.schoolName)
                    put("schoolNpsn", profile.schoolNpsn)
                    put("schoolAddress", profile.schoolAddress)
                    put("educationLevel", profile.educationLevel)
                    put("defaultPhase", profile.defaultPhase)
                    put("defaultSubject", profile.defaultSubject)
                    put("academicYear", profile.academicYear)
                    put("principalName", profile.principalName)
                    put("principalNip", profile.principalNip)
                })
            }
            val docsArray = JSONArray()
            documents.forEach { doc ->
                docsArray.put(JSONObject().apply {
                    put("id", doc.id)
                    put("docType", doc.docType)
                    put("title", doc.title)
                    put("educationLevel", doc.educationLevel)
                    put("phase", doc.phase)
                    put("grade", doc.grade)
                    put("subject", doc.subject)
                    put("topic", doc.topic)
                    put("duration", doc.duration)
                    put("pedagogicalModel", doc.pedagogicalModel)
                    put("deepLearningPrinciples", JSONArray(doc.deepLearningPrinciples))
                    put("graduateDimensions", JSONArray(doc.graduateDimensions))
                    put("contentMarkdown", doc.contentMarkdown)
                    put("starred", doc.starred)
                })
            }
            put("documents", docsArray)
        }
        return root.toString(2)
    }

    fun deserialize(rawJson: String): Result<BackupBundle> {
        return try {
            val root = JSONObject(rawJson)
            val version = root.optInt("version", 1)
            val exportedAt = root.optString("exportedAt", "")
            val attribution = root.optString("attribution", TeacherProfile.POWERED_BY_ATTRIBUTION)

            val profileObj = root.optJSONObject("profile")
            val profile = profileObj?.let { p ->
                TeacherProfile(
                    teacherName = p.optString("teacherName", TeacherProfile.DEFAULT_TEACHER_NAME),
                    nip = p.optString("nip", TeacherProfile.DEFAULT_TEACHER_NIP),
                    schoolName = p.optString("schoolName", TeacherProfile.DEFAULT_SCHOOL_NAME),
                    schoolNpsn = p.optString("schoolNpsn", TeacherProfile.DEFAULT_SCHOOL_NPSN),
                    schoolAddress = p.optString("schoolAddress", TeacherProfile.DEFAULT_SCHOOL_ADDRESS),
                    educationLevel = p.optString("educationLevel", "SMP/MTs"),
                    defaultPhase = p.optString("defaultPhase", "Fase D (Kelas 7-9)"),
                    defaultSubject = p.optString("defaultSubject", "IPA"),
                    academicYear = p.optString("academicYear", "2026/2027"),
                    principalName = p.optString("principalName", TeacherProfile.DEFAULT_PRINCIPAL_NAME),
                    principalNip = p.optString("principalNip", TeacherProfile.DEFAULT_PRINCIPAL_NIP)
                )
            }

            val docsArray = root.optJSONArray("documents")
                ?: return Result.failure(IllegalArgumentException("Format file backup tidak valid: array 'documents' tidak ditemukan."))

            val docs = mutableListOf<TeachingDocument>()
            for (i in 0 until docsArray.length()) {
                val item = docsArray.getJSONObject(i)
                val principlesArr = item.optJSONArray("deepLearningPrinciples")
                val principles = if (principlesArr != null) {
                    (0 until principlesArr.length()).mapNotNull { principlesArr.optString(it).takeIf(String::isNotBlank) }
                } else emptyList()

                val dimsArr = item.optJSONArray("graduateDimensions")
                val dims = if (dimsArr != null) {
                    (0 until dimsArr.length()).mapNotNull { dimsArr.optString(it).takeIf(String::isNotBlank) }
                } else emptyList()

                val rawType = item.optString("docType", DocType.MODUL_AJAR.name)
                val title = item.optString("title", "").trim()
                val content = item.optString("contentMarkdown", "").trim()
                if (title.isNotEmpty() && content.isNotEmpty()) {
                    docs.add(
                        TeachingDocument(
                            id = item.optString("id", ""),
                            docType = DocType.fromRaw(rawType).name,
                            title = title,
                            educationLevel = item.optString("educationLevel", "SD/MI"),
                            phase = item.optString("phase", "Fase C (Kelas 5-6)"),
                            grade = item.optString("grade", ""),
                            subject = item.optString("subject", "Umum"),
                            topic = item.optString("topic", title),
                            duration = item.optString("duration", ""),
                            pedagogicalModel = item.optString("pedagogicalModel", "Project-Based Learning (PjBL)"),
                            deepLearningPrinciples = principles,
                            graduateDimensions = dims,
                            contentMarkdown = content,
                            starred = item.optBoolean("starred", false)
                        )
                    )
                }
            }

            if (docs.isEmpty() && profile == null) {
                return Result.failure(IllegalArgumentException("File backup kosong atau tidak berisi perangkat ajar."))
            }

            Result.success(
                BackupBundle(
                    version = version,
                    exportedAt = exportedAt,
                    attribution = attribution,
                    profile = profile,
                    documents = docs
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Gagal membaca file backup JSON: ${e.localizedMessage ?: "Format rusak"}"))
        }
    }
}
