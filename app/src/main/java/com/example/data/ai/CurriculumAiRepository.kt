package com.example.data.ai

import com.example.data.model.DocType
import com.example.data.model.TeacherProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

data class GenerationRequest(
    val docType: DocType,
    val educationLevel: String,
    val phase: String,
    val grade: String,
    val subject: String,
    val topic: String,
    val duration: String,
    val pedagogicalModel: String,
    val deepLearningPrinciples: List<String>,
    val graduateDimensions: List<String>,
    val localContextNotes: String,
    val includeSchoolHeader: Boolean,
    val profile: TeacherProfile
)

class CurriculumAiRepository {
    private val model by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    fun generateDocumentStream(request: GenerationRequest): Flow<String> {
        val prompt = buildKemendikdasmenPrompt(request)
        return model.generateContentStream(prompt).mapNotNull { chunk -> chunk.text }
    }

    private fun buildKemendikdasmenPrompt(req: GenerationRequest): String {
        val principlesText = req.deepLearningPrinciples.ifEmpty {
            listOf("Berkesadaran (Mindful)", "Bermakna (Meaningful)", "Menggembirakan (Joyful)")
        }.joinToString(", ")

        val dimensionsText = req.graduateDimensions.ifEmpty {
            listOf("Penalaran Kritis", "Kreativitas", "Kolaborasi", "Komunikasi")
        }.joinToString(", ")

        val identitySection = if (req.includeSchoolHeader) {
            """
            IDENTITAS RESMI PENYUSUN (Sertakan rapi di bagian paling atas dan blok pengesahan di bawah):
            - Satuan Pendidikan: ${req.profile.schoolName}
            - NPSN: ${req.profile.schoolNpsn}
            - Alamat Sekolah: ${req.profile.schoolAddress}
            - Nama Penyusun / Guru: ${req.profile.teacherName} (NIP. ${req.profile.nip})
            - Kepala Sekolah: ${req.profile.principalName} (NIP. ${req.profile.principalNip})
            - Tahun Ajaran: ${req.profile.academicYear}
            """.trimIndent()
        } else {
            "IDENTITAS PENYUSUN: Cantumkan placeholder identitas guru & sekolah yang siap diisi."
        }

        val specificStructure = when (req.docType) {
            DocType.MODUL_AJAR -> """
                Buat **MODUL AJAR KURIKULUM MERDEKA PENDEKATAN PEMBELAJARAN MENDALAM (DEEP LEARNING) KEMENDIKDASMEN** yang lengkap dan siap pakai:
                1. **Header & Identitas Modul** (Satuan Pendidikan, Penyusun, NIP, Jenjang/Fase/Kelas, Mapel, Topik, Alokasi Waktu, Tahun Ajaran).
                2. **Identifikasi Kesiapan Peserta Didik & Kompetensi Awal** (Analisis kesiapan kognitif, afektif, dan sosial-emosional).
                3. **8 Dimensi Profil Lulusan Kemendikdasmen** (Jelaskan secara konkret integrasi dimensi terpilih: $dimensionsText).
                4. **Desain Pembelajaran**:
                   - Capaian Pembelajaran (CP) relevan dengan fase ${req.phase}.
                   - Tujuan Pembelajaran (TP) terukur & Alur Tujuan Pembelajaran singkat.
                   - Pemahaman Bermakna & Pertanyaan Pemantik (HOTS).
                   - Kerangka Pembelajaran Mendalam (4 Pilar): Praktik Pedagogis (${req.pedagogicalModel}), Kemitraan Pembelajaran, Lingkungan Pembelajaran, Pemanfaatan Digital.
                5. **Pengalaman Belajar (Langkah Pembelajaran Deep Learning)**:
                   Integrasikan secara eksplisit Prinsip **$principlesText** ke dalam tiga tahapan utama:
                   - **Kegiatan Awal / Orientasi Berkesadaran** (Mindful opening, apersepsi bermakna, asesmen awal).
                   - **Inti Tahap 1: Memahami** (Eksplorasi konsep esensial & aplikatif).
                   - **Inti Tahap 2: Mengaplikasi** (Pemecahan masalah / proyek nyata kontekstual & diferensiasi proses/produk).
                   - **Inti Tahap 3: Merefleksi** (Regulasi diri, evaluasi proses belajar, makna bagi kehidupan).
                   - **Penutup** (Kesimpulan kolaboratif, umpan balik, tindak lanjut).
                6. **Asesmen Pembelajaran Mendalam**:
                   - Asesmen Awal (Diagnostik kognitif & non-kognitif).
                   - Asesmen Proses (Formatif observasi & umpan balik).
                   - Asesmen Akhir (Sumatif + Rubrik Kriteria Ketercapaian Tujuan Pembelajaran / KKTP dengan 4 tingkat: Perlu Bimbingan, Cukup, Baik, Sangat Baik).
                7. **Pengayaan, Remedial & Lampiran Ringkas** (Glosarium & Bahan Bacaan).
            """.trimIndent()

            DocType.CP_TP_ATP -> """
                Buat dokumen **ANALISIS CAPAIAN PEMBELAJARAN (CP), TUJUAN PEMBELAJARAN (TP), DAN ALUR TUJUAN PEMBELAJARAN (ATP)** sesuai Kurikulum Merdeka & Pembelajaran Mendalam Kemendikdasmen:
                1. **Identitas Dokumen** (Satuan Pendidikan, Guru, Fase, Mapel, Tahun Ajaran).
                2. **Rumusan Capaian Pembelajaran (CP)** per Elemen untuk ${req.subject} ${req.phase}.
                3. **Analisis Kompetensi & Konten Esensial** (Pemetaan konsep inti yang mendukung Deep Learning).
                4. **Daftar Tujuan Pembelajaran (TP)** terstruktur dengan integrasi prinsip $principlesText dan Dimensi Profil Lulusan ($dimensionsText).
                5. **Alur Tujuan Pembelajaran (ATP) Berurutan**: susun dalam daftar bernomor/tabel teks rapi berisi Kode TP, Rumusan TP, Pengalaman Belajar (Memahami/Mengaplikasi/Merefleksi), Alokasi JP, serta Indikator Asesmen Kunci.
            """.trimIndent()

            DocType.LKPD -> """
                Buat **LEMBAR KERJA PESERTA DIDIK (LKPD) PEMBELAJARAN MENDALAM** yang interaktif, ramah siswa, dan langsung dapat dicetak:
                1. **Kop & Identitas LKPD** (Sekolah, Mapel, Kelas/Fase, Topik, Nama Siswa/Kelompok, Alokasi Waktu).
                2. **Tujuan Pembelajaran & Dimensi Profil Lulusan** ($dimensionsText).
                3. **Petunjuk Belajar Berkesadaran (Mindful Guide)** untuk peserta didik.
                4. **Aktivitas 1 — Memahami (Eksplorasi Konsep)**: Pertanyaan pemantik, bacaan/studi kasus kontekstual, dan tabel pengamatan.
                5. **Aktivitas 2 — Mengaplikasi (Tantangan Nyata / Proyek Mini)**: Langkah kerja kolaboratif, ruang pemecahan masalah berbasis ${req.pedagogicalModel}.
                6. **Aktivitas 3 — Merefleksi (Jurnal Pemaknaan Diri)**: Pertanyaan metakognitif tentang apa yang dipahami, perasaan saat belajar (Joyful), dan manfaat nyata (Meaningful).
                7. **Rubrik Penilaian Diri & Penilaian Guru**.
            """.trimIndent()

            DocType.ASESMEN -> """
                Buat **PAKET INSTRUMEN & RUBRIK ASESMEN PEMBELAJARAN MENDALAM (DEEP LEARNING)**:
                1. **Identitas Asesmen** (Sekolah, Penyusun, Mapel, Fase/Kelas, Topik).
                2. **Peta Ketercapaian Tujuan Pembelajaran (KKTP)** berbasis deskripsi kriteria dan interval nilai.
                3. **Instrumen Asesmen Awal (Diagnostik)** untuk memetakan kesiapan belajar & gaya belajar.
                4. **Instrumen Asesmen Proses (Formatif)**: Lembar observasi kolaborasi, penalaran kritis, dan ceklis umpan balik.
                5. **Kisi-Kisi & Soal Asesmen Sumatif (Berbasis HOTS & Taksonomi SOLO: Unistructural, Multistructural, Relational, Extended Abstract)** lengkap dengan **Kunci Jawaban / Pedoman Penskoran** (minimal 5 soal kontekstual + 2 soal uraian pemecahan masalah).
                6. **Rubrik Penilaian Analitik 4 Level** (Perlu Bimbingan, Cukup, Baik, Sangat Baik).
            """.trimIndent()

            DocType.PROSEM_PROTA -> """
                Buat **PROGRAM TAHUNAN (PROTA) & PROGRAM SEMESTER (PROSEM)** Kurikulum Merdeka Terintegrasi Deep Learning:
                1. **Identitas Perangkat** (Sekolah, Penyusun, NIP, Mapel, Fase/Kelas, Tahun Ajaran).
                2. **Perhitungan Minggu Efektif & Alokasi Jam Pelajaran (JP)** Semester Ganjil & Genap.
                3. **Program Tahunan (Prota)**: Pemetaan seluruh lingkup materi/bab utama dalam setahun beserta Alokasi JP dan Dimensi Profil Lulusan.
                4. **Program Semester (Prosem)**: Rincian distribusi TP/Topik (${req.topic} sebagai fokus utama beserta materi terkait) per bulan dan pekan, jadwal Asesmen Formatif, Sumatif Lingkup Materi, Projek Kokurikuler, dan Asesmen Sumatif Akhir Semester.
            """.trimIndent()

            DocType.KOKURIKULER_P5 -> """
                Buat **MODUL PROJEK KOKURIKULER / PENGUATAN PROFIL LULUSAN (P5 & DEEP LEARNING KEMENDIKDASMEN)**:
                1. **Identitas Projek** (Sekolah, Koordinator/Guru, Fase/Kelas, Tema Lintas Disiplin, Topik: ${req.topic}, Alokasi JP: ${req.duration}).
                2. **Tujuan & Relevansi Projek** dengan konteks lokal dan 8 Dimensi Profil Lulusan ($dimensionsText).
                3. **Integrasi 3 Prinsip Deep Learning** ($principlesText) & **4 Kerangka Pembelajaran** (Praktik Pedagogis, Kemitraan Komunitas/Orang Tua, Lingkungan Belajar, Digital).
                4. **Alur Tahapan Projek (Memahami -> Mengaplikasi -> Merefleksi)**:
                   - Tahap Pengenalan & Eksplorasi Masalah
                   - Tahap Kontekstualisasi & Perancangan Solusi
                   - Tahap Aksi Nyata & Kolaborasi
                   - Tahap Refleksi, Perayaan Belajar & Tindak Lanjut
                5. **Rubrik Perkembangan Dimensi Profil Lulusan** (Mulai Berkembang, Sedang Berkembang, Berkembang Sesuai Harapan, Sangat Berkembang).
            """.trimIndent()

            DocType.JURNAL_REFLEKSI -> """
                Buat **JURNAL REFLEKSI PEMBELAJARAN MENDALAM (GURU & PESERTA DIDIK)**:
                1. **Identitas Pertemuan** (Sekolah, Guru, Mapel, Fase/Kelas, Topik, Tanggal).
                2. **Refleksi Keterlaksanaan 3 Prinsip Deep Learning**:
                   - Evaluasi aspek **Berkesadaran (Mindful Learning)**
                   - Evaluasi aspek **Bermakna (Meaningful Learning)**
                   - Evaluasi aspek **Menggembirakan (Joyful Learning)**
                3. **Refleksi Pengalaman Belajar (Memahami, Mengaplikasi, Merefleksi)** & Diferensiasi Murid.
                4. **Instrumen Lembar Refleksi Peserta Didik** (Model 4F: Facts, Feelings, Findings, Future — atau 3-2-1 Deep Reflection).
                5. **Analisis Tindak Lanjut & Rencana Perbaikan Pertemuan Berikutnya**.
            """.trimIndent()
        }

        return """
Anda adalah Pakar Pengembang Kurikulum Merdeka dan Pendekatan Pembelajaran Mendalam (Deep Learning) resmi sesuai kebijakan terbaru Kemendikdasmen Republik Indonesia.

Tugas Anda adalah menyusun perangkat mengajar berbahasa Indonesia yang baku, pedagogis, terstruktur rapi dengan format Markdown bersih (`#`, `##`, `###`, poin `-`, nomor `1.`, dan penekanan `**tebal**`), serta langsung siap diterapkan atau dicetak oleh guru.

SPESIFIKASI INPUT:
- Jenis Perangkat: ${req.docType.label}
- Jenjang & Fase: ${req.educationLevel} — ${req.phase}
- Kelas / Semester: ${req.grade}
- Mata Pelajaran: ${req.subject}
- Topik / Materi Pokok: ${req.topic}
- Alokasi Waktu: ${req.duration}
- Model Pedagogis: ${req.pedagogicalModel}
- Fokus Prinsip Deep Learning: $principlesText
- Dimensi Profil Lulusan: $dimensionsText
- Catatan Konteks Lokal / Kebutuhan Murid: ${req.localContextNotes.ifBlank { "Konteks sekolah Indonesia yang beragam, mendukung pembelajaran inklusif dan berdiferensiasi." }}

$identitySection

STRUKTUR WAJIB YANG HARUS DIHASILKAN:
$specificStructure

PENUTUP WAJIB DI BAGIAN AKHIR DOKUMEN:
Sertakan blok **Lembar Pengesahan** rapi dengan dua kolom teks sejajar atau terstruktur untuk:
- Mengetahui, Kepala Sekolah: ${req.profile.principalName} (NIP. ${req.profile.principalNip})
- Guru Mata Pelajaran / Kelas: ${req.profile.teacherName} (NIP. ${req.profile.nip})
Dan akhiri dokumen dengan baris footer persis berikut:
`---`
`*${TeacherProfile.POWERED_BY_ATTRIBUTION}*`
        """.trimIndent()
    }
}
