package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class DocType(
    val label: String,
    val shortLabel: String,
    val subtitle: String,
    val badgeText: String
) {
    MODUL_AJAR(
        label = "Modul Ajar Deep Learning",
        shortLabel = "Modul Ajar",
        subtitle = "RPP+ lengkap 8 Dimensi Profil Lulusan, 3 Prinsip & 3 Pengalaman Belajar",
        badgeText = "RPP+"
    ),
    CP_TP_ATP(
        label = "CP, TP & ATP",
        shortLabel = "CP / ATP",
        subtitle = "Analisis Capaian Pembelajaran ke Tujuan & Alur Tujuan Pembelajaran",
        badgeText = "ATP"
    ),
    LKPD(
        label = "Lembar Kerja Peserta Didik",
        shortLabel = "LKPD",
        subtitle = "Aktivitas terstruktur Memahami, Mengaplikasi & Merefleksi",
        badgeText = "LKPD"
    ),
    ASESMEN(
        label = "Instrumen & Rubrik Asesmen",
        shortLabel = "Asesmen",
        subtitle = "Asesmen awal, formatif, sumatif, KKTP & kisi-kisi soal HOTS",
        badgeText = "Rubrik"
    ),
    PROSEM_PROTA(
        label = "Program Tahunan & Semester",
        shortLabel = "Prota/Prosem",
        subtitle = "Pemetaan alokasi waktu JP, pekan efektif & kalender asesmen",
        badgeText = "Prosem"
    ),
    KOKURIKULER_P5(
        label = "Projek Kokurikuler & P5",
        shortLabel = "Kokurikuler",
        subtitle = "Desain projek lintas disiplin berbasis Profil Lulusan Kemendikdasmen",
        badgeText = "Projek"
    ),
    JURNAL_REFLEKSI(
        label = "Jurnal Refleksi Pembelajaran",
        shortLabel = "Refleksi",
        subtitle = "Refleksi guru & peserta didik berbasis Mindful-Meaningful-Joyful",
        badgeText = "Jurnal"
    );

    companion object {
        fun fromRaw(raw: String?): DocType =
            entries.firstOrNull { it.name == raw } ?: MODUL_AJAR
    }
}

@Keep
@IgnoreExtraProperties
data class TeacherProfile(
    val userId: String = "",
    val teacherName: String = DEFAULT_TEACHER_NAME,
    val nip: String = DEFAULT_TEACHER_NIP,
    val schoolName: String = DEFAULT_SCHOOL_NAME,
    val schoolNpsn: String = DEFAULT_SCHOOL_NPSN,
    val schoolAddress: String = DEFAULT_SCHOOL_ADDRESS,
    val educationLevel: String = "SMP/MTs",
    val defaultPhase: String = "Fase D (Kelas 7-9)",
    val defaultSubject: String = "IPA",
    val academicYear: String = "2026/2027",
    val principalName: String = DEFAULT_PRINCIPAL_NAME,
    val principalNip: String = DEFAULT_PRINCIPAL_NIP,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    companion object {
        const val DEFAULT_TEACHER_NAME = "Ilyas, S.Pd."
        const val DEFAULT_TEACHER_NIP = "196908052007011026"
        const val DEFAULT_SCHOOL_NAME = "SMPN 1 GONDANG KAB. MOJOKERTO"
        const val DEFAULT_SCHOOL_NPSN = "20502646"
        const val DEFAULT_SCHOOL_ADDRESS = "Jalan Raya Karangkuten, Kec. Gondang, Kab. Mojokerto, 61372"
        const val DEFAULT_PRINCIPAL_NAME = "Subai, S.Pd."
        const val DEFAULT_PRINCIPAL_NIP = "196812071997031003"
        const val POWERED_BY_ATTRIBUTION = "Powered by Ilyas Auliya Alqarany @2026"

        fun sampleDefault(userId: String = ""): TeacherProfile = TeacherProfile(
            userId = userId,
            teacherName = DEFAULT_TEACHER_NAME,
            nip = DEFAULT_TEACHER_NIP,
            schoolName = DEFAULT_SCHOOL_NAME,
            schoolNpsn = DEFAULT_SCHOOL_NPSN,
            schoolAddress = DEFAULT_SCHOOL_ADDRESS,
            educationLevel = "SMP/MTs",
            defaultPhase = "Fase D (Kelas 7-9)",
            defaultSubject = "IPA",
            academicYear = "2026/2027",
            principalName = DEFAULT_PRINCIPAL_NAME,
            principalNip = DEFAULT_PRINCIPAL_NIP
        )
    }
}

@Keep
@IgnoreExtraProperties
data class TeachingDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val docType: String = DocType.MODUL_AJAR.name,
    val title: String = "",
    val educationLevel: String = "SD/MI",
    val phase: String = "Fase C (Kelas 5-6)",
    val grade: String = "Kelas 5 / Semester 1",
    val subject: String = "",
    val topic: String = "",
    val duration: String = "2 x 35 Menit (1 Pertemuan)",
    val pedagogicalModel: String = "Project-Based Learning (PjBL)",
    val deepLearningPrinciples: List<String> = emptyList(),
    val graduateDimensions: List<String> = emptyList(),
    val contentMarkdown: String = "",
    val starred: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun safeDocType(): DocType = DocType.fromRaw(docType)
}

@Keep
@IgnoreExtraProperties
data class BackupLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actionType: String = "EXPORT_JSON",
    val itemCount: Long = 0L,
    val note: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudBackupSnapshot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val snapshotName: String = "",
    val itemCount: Long = 0L,
    val payloadJson: String = "",
    val createdAt: Timestamp? = null
)

object DeepLearningCatalog {
    data class PhasePreset(
        val level: String,
        val phase: String,
        val sampleGrades: List<String>
    )

    val phasePresets = listOf(
        PhasePreset("PAUD", "Fase Fondasi", listOf("Kelompok A (4-5 Th)", "Kelompok B (5-6 Th)")),
        PhasePreset("SD/MI", "Fase A (Kelas 1-2)", listOf("Kelas 1 / Semester 1", "Kelas 2 / Semester 1")),
        PhasePreset("SD/MI", "Fase B (Kelas 3-4)", listOf("Kelas 3 / Semester 1", "Kelas 4 / Semester 1")),
        PhasePreset("SD/MI", "Fase C (Kelas 5-6)", listOf("Kelas 5 / Semester 1", "Kelas 6 / Semester 1")),
        PhasePreset("SMP/MTs", "Fase D (Kelas 7-9)", listOf("Kelas 7 / Semester 1", "Kelas 8 / Semester 1", "Kelas 9 / Semester 1")),
        PhasePreset("SMA/MA", "Fase E (Kelas 10)", listOf("Kelas 10 / Semester 1", "Kelas 10 / Semester 2")),
        PhasePreset("SMA/MA", "Fase F (Kelas 11-12)", listOf("Kelas 11 / Semester 1", "Kelas 12 / Semester 1")),
        PhasePreset("SMK/MAK", "Fase E-F (Kejuruan)", listOf("Kelas 10 / Semester 1", "Kelas 11 / Semester 1", "Kelas 12 / Semester 1"))
    )

    val principles = listOf(
        "Berkesadaran (Mindful)",
        "Bermakna (Meaningful)",
        "Menggembirakan (Joyful)"
    )

    val learningExperiences = listOf(
        "Memahami" to "Konstruksi pengetahuan esensial, aplikatif, dan humanis secara aktif.",
        "Mengaplikasi" to "Pendalaman pengetahuan dalam konteks nyata lintas disiplin ilmu.",
        "Merefleksi" to "Evaluasi diri, regulasi belajar, dan pemaknaan proses pembelajaran."
    )

    val frameworkPillars = listOf(
        "Praktik Pedagogis" to "Strategi inkuiri, PjBL, PBL, dan diferensiasi yang menantang nalar.",
        "Kemitraan Pembelajaran" to "Kolaborasi guru, murid, orang tua, komunitas, dan dunia kerja.",
        "Lingkungan Pembelajaran" to "Budaya belajar aman, inklusif, fisik dan virtual yang fleksibel.",
        "Pemanfaatan Digital" to "Teknologi interaktif untuk eksplorasi, kreasi, dan asesmen autentik."
    )

    val graduateDimensions = listOf(
        "Keimanan & Ketakwaan",
        "Kewargaan",
        "Penalaran Kritis",
        "Kreativitas",
        "Kolaborasi",
        "Kemandirian",
        "Kesehatan",
        "Komunikasi"
    )

    val pedagogicalModels = listOf(
        "Project-Based Learning (PjBL)",
        "Problem-Based Learning (PBL)",
        "Inkuiri & Discovery Learning",
        "Pembelajaran Berdiferensiasi",
        "Cooperative & Contextual Learning"
    )

    val sampleSubjects = listOf(
        "IPAS",
        "Matematika",
        "Bahasa Indonesia",
        "Pendidikan Pancasila",
        "Bahasa Inggris",
        "IPA",
        "IPS",
        "Informatika",
        "Pendidikan Agama Islam",
        "PJOK",
        "Seni Budaya"
    )
}
