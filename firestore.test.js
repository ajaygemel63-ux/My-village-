const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_villager_1";
const BOB_UID = "bob_villager_2";
const ADMIN_UID = "admin_sarpanch_1";
const ADMIN_EMAIL = "ajayhoney795@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("members").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("families").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("village_config").doc("main").get());
  await assertFails(
    unauthDb.collection("members").doc("mem_1").set({
      ownerUid: ALICE_UID,
      fullName: "Test",
    })
  );
});

test("Villager cannot self-assign admin or edit village_config", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      roleLabel: "ADMIN",
      addedByUid: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("village_config").doc("main").set({
      villageNameHi: "गाँव",
      villageNameOr: "ଗାଁ",
      villageNameEn: "Village",
      wards: ["Ward 1"],
      updatedByUid: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Email spoofing with unverified admin email is rejected", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    spoofDb.collection("village_config").doc("main").set({
      villageNameHi: "सुंदरपुर",
      villageNameOr: "ସୁନ୍ଦରପୁର",
      villageNameEn: "Sundarpur",
      wards: ["Ward 1"],
      updatedByUid: "spoof_uid",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Verified bootstrapped admin can update village_config and verify records", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    adminDb.collection("village_config").doc("main").set({
      villageNameHi: "सुंदरपुर ग्राम",
      villageNameOr: "ସୁନ୍ଦରପୁର ଗାଁ",
      villageNameEn: "Sundarpur Village",
      district: "Cuttack",
      stateName: "Odisha",
      wards: ["Ward 1 / वार्ड 1", "Ward 2 / वार्ड 2"],
      updatedByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Villager can read village_config/main
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("village_config").doc("main").get());
});

test("Family & Member registration, masked IDs, and private record isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Alice creates a PRIVATE family
  await assertSucceeds(
    aliceDb.collection("families").doc("fam_1").set({
      ownerUid: ALICE_UID,
      familyCode: "FAM-101",
      headName: "Ramesh Das",
      headNameLocal: "रमेश दास / ରମେଶ ଦାସ",
      ward: "Ward 1",
      houseNumber: "H-12",
      rationCardCategory: "BPL",
      memberCount: 4,
      visibility: "PRIVATE",
      authorizedViewerUids: [],
      verificationStatus: "PENDING",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice creates a PRIVATE member linked to fam_1 with masked Aadhaar & Voter ID (NO @DocumentId id field)
  await assertSucceeds(
    aliceDb.collection("members").doc("mem_1").set({
      ownerUid: ALICE_UID,
      familyId: "fam_1",
      familyCode: "FAM-101",
      fullName: "Ramesh Das",
      fullNameLocal: "रमेश दास",
      guardianName: "Mohan Das",
      relationToHead: "HEAD",
      age: 42,
      gender: "MALE",
      ward: "Ward 1",
      occupation: "Farmer",
      aadhaarMasked: "XXXX-XXXX-4829",
      aadhaarLast4: "4829",
      voterIdMasked: "ODJ••••741",
      voterIdLast4: "741",
      visibility: "PRIVATE",
      authorizedViewerUids: [],
      verificationStatus: "PENDING",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice writes isolated private_details/info
  await assertSucceeds(
    aliceDb.collection("members").doc("mem_1").collection("private_details").doc("info").set({
      ownerUid: ALICE_UID,
      authorizedViewerUids: [],
      phoneNumber: "9876543210",
      fullAddress: "Plot 12, Near Banyan Tree, Ward 1",
      emergencyContact: "Sita Das - 9876500001",
      privateNotes: "Diabetic checkup monthly",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Shadow field or self-verification rejected
  await assertFails(
    aliceDb.collection("members").doc("mem_2").set({
      ownerUid: ALICE_UID,
      fullName: "Self Verified",
      guardianName: "Test",
      relationToHead: "HEAD",
      age: 30,
      gender: "MALE",
      ward: "Ward 1",
      aadhaarMasked: "XXXX-XXXX-1111",
      voterIdMasked: "ODJ••••111",
      visibility: "PUBLIC",
      authorizedViewerUids: [],
      verificationStatus: "VERIFIED",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob CANNOT read Alice's private family, private member, or private_details
  await assertFails(bobDb.collection("families").doc("fam_1").get());
  await assertFails(bobDb.collection("members").doc("mem_1").get());
  await assertFails(bobDb.collection("members").doc("mem_1").collection("private_details").doc("info").get());

  // Unfiltered collection list query must fail
  await assertFails(bobDb.collection("members").get());
  await assertFails(bobDb.collection("families").get());

  // Filtered public query succeeds
  await assertSucceeds(bobDb.collection("members").where("visibility", "==", "PUBLIC").get());
  await assertSucceeds(bobDb.collection("families").where("visibility", "==", "PUBLIC").get());
});

test("Permission Request flow grants Bob access to private member upon owner approval", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("members").doc("mem_priv").set({
      ownerUid: ALICE_UID,
      familyId: "",
      familyCode: "",
      fullName: "Sita Devi",
      guardianName: "Ramesh Das",
      relationToHead: "SPOUSE",
      age: 38,
      gender: "FEMALE",
      ward: "Ward 2",
      occupation: "Teacher",
      aadhaarMasked: "XXXX-XXXX-9988",
      aadhaarLast4: "9988",
      voterIdMasked: "ODJ••••321",
      voterIdLast4: "321",
      visibility: "PRIVATE",
      authorizedViewerUids: [],
      verificationStatus: "PENDING",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("members").doc("mem_priv").collection("private_details").doc("info").set({
      ownerUid: ALICE_UID,
      authorizedViewerUids: [],
      phoneNumber: "9998887776",
      fullAddress: "Ward 2",
      emergencyContact: "Ramesh",
      privateNotes: "School coordinator",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob requests permission
  await assertSucceeds(
    bobDb.collection("access_requests").doc("req_1").set({
      requesterUid: BOB_UID,
      requesterName: "Bob Behera",
      targetType: "MEMBER",
      targetId: "mem_priv",
      targetName: "Sita Devi",
      targetOwnerUid: ALICE_UID,
      reason: "Panchayat health survey",
      status: "PENDING",
      reviewedByUid: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob cannot approve his own request
  await assertFails(
    bobDb.collection("access_requests").doc("req_1").update({
      status: "APPROVED",
      reviewedByUid: BOB_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice approves and adds Bob to authorizedViewerUids
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("access_requests").doc("req_1").update({
      status: "APPROVED",
      reviewedByUid: ALICE_UID,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("members").doc("mem_priv").update({
      authorizedViewerUids: [BOB_UID],
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("members").doc("mem_priv").collection("private_details").doc("info").update({
      authorizedViewerUids: [BOB_UID],
      updatedAt: now,
    })
  );

  // Now Bob CAN read mem_priv, query shared members, and read private_details/info!
  await assertSucceeds(bobDb.collection("members").doc("mem_priv").get());
  await assertSucceeds(bobDb.collection("members").where("authorizedViewerUids", "array-contains", BOB_UID).get());
  await assertSucceeds(bobDb.collection("members").doc("mem_priv").collection("private_details").doc("info").get());
});
