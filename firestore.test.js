const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-ajarmerd-5107e30f-d925-4f5f-adec-b33727675de7";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    teacherName: "Ilyas, S.Pd.",
    nip: "196908052007011026",
    schoolName: "SMPN 1 GONDANG KAB. MOJOKERTO",
    schoolNpsn: "20502646",
    schoolAddress: "Jalan Raya Karangkuten, Kec. Gondang, Kab. Mojokerto, 61372",
    educationLevel: "SMP/MTs",
    defaultPhase: "Fase D (Kelas 7-9)",
    defaultSubject: "IPA",
    academicYear: "2026/2027",
    principalName: "Subai, S.Pd.",
    principalNip: "196812071997031003",
    createdAt: now,
    updatedAt: now,
  };
}

function validTeachingDocPayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    docType: "MODUL_AJAR",
    title: "Modul Ajar Ekosistem Berbasis Deep Learning",
    educationLevel: "SD/MI",
    phase: "Fase C (Kelas 5-6)",
    grade: "Kelas 5 / Semester 1",
    subject: "IPAS",
    topic: "Keseimbangan Ekosistem dan Rantai Makanan",
    duration: "2 x 35 Menit (1 Pertemuan)",
    pedagogicalModel: "Project-Based Learning (PjBL)",
    deepLearningPrinciples: ["Berkesadaran", "Bermakna", "Menggembirakan"],
    graduateDimensions: ["Penalaran Kritis", "Kolaborasi", "Kreativitas"],
    contentMarkdown: "# MODUL AJAR DEEP LEARNING\nKonten lengkap perangkat ajar.",
    starred: false,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write profile, teaching_docs, or backup_logs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("teaching_docs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("profile").doc("teacher").set(validProfilePayload(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("teaching_docs").doc("doc_1").set(validTeachingDocPayload(ALICE_UID)));
});

test("Owner can create, read, partially update, and delete teaching_docs matching Kotlin payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("teaching_docs").doc("doc_1").set(
      validTeachingDocPayload(ALICE_UID, created)
    )
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("teaching_docs").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("teaching_docs").doc("doc_1").update({
      starred: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("teaching_docs").doc("doc_1").update({
      title: "Modul Ajar Ekosistem Revisi",
      contentMarkdown: "# MODUL AJAR REVISI",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("teaching_docs").doc("doc_1").update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject @DocumentId `id` field inside document payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("teaching_docs").doc("doc_2").set({
      ...validTeachingDocPayload(ALICE_UID, created),
      id: "doc_2",
    })
  );

  // Reject invalid docType enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("teaching_docs").doc("doc_3").set({
      ...validTeachingDocPayload(ALICE_UID, created),
      docType: "INVALID_TYPE",
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("teaching_docs").doc("doc_4").set(
      validTeachingDocPayload(ALICE_UID, new Date(Date.now() + 60000))
    )
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("teaching_docs").doc("doc_1").delete()
  );
});

test("Owner can manage teacher profile and backup logs; cross-user access is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("teacher").set(
      validProfilePayload(ALICE_UID, created)
    )
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("teacher").update({
      schoolName: "SDN 2 Merdeka",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject wrong profileId
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("other").set(
      validProfilePayload(ALICE_UID, created)
    )
  );

  // Backup log create succeeds, update denied
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("backup_logs").doc("log_1").set({
      userId: ALICE_UID,
      actionType: "EXPORT_JSON",
      itemCount: 3,
      note: "Ekspor backup lokal JSON",
      createdAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("backup_logs").doc("log_1").update({
      itemCount: 99,
    })
  );

  // Bob cannot read or write Alice's collections
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("profile").doc("teacher").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("teaching_docs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("backup_logs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cloud_backups").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("teaching_docs").doc("hijack").set(
      validTeachingDocPayload(ALICE_UID, created)
    )
  );
});

test("Owner can create, list, and delete cloud_backups snapshots; updates and cross-user access denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  const created = new Date(Date.now() - 4000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloud_backups").doc("snap_1").set({
      userId: ALICE_UID,
      snapshotName: "Cadangan Semester Ganjil",
      itemCount: 2,
      payloadJson: JSON.stringify({ version: 1, documents: [] }),
      createdAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloud_backups").get()
  );

  // Snapshots are immutable
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloud_backups").doc("snap_1").update({
      snapshotName: "Diubah",
    })
  );

  // Bob cannot create or delete Alice's snapshots
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cloud_backups").doc("snap_bob").set({
      userId: ALICE_UID,
      snapshotName: "Hijack",
      itemCount: 0,
      payloadJson: "{}",
      createdAt: created,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cloud_backups").doc("snap_1").delete()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloud_backups").doc("snap_1").delete()
  );
});
