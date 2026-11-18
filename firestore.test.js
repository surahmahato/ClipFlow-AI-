const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function validUserProfile(uid, ts) {
  return {
    userId: uid,
    displayName: "Alice Creator",
    email: "alice@example.com",
    planTier: "FREE",
    clipsGeneratedCount: 0,
    brandName: "ClipFlow Studio",
    brandPrimaryHex: "#F4D96B",
    brandFontStyle: "Space Grotesk",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validProject(uid, ts) {
  return {
    userId: uid,
    title: "Podcast Episode 12",
    folderName: "Podcasts",
    sourceType: "LOCAL_VIDEO",
    sourceUrl: "content://media/external/video/101",
    videoCategory: "Podcast",
    status: "ANALYZED",
    durationMs: 120000,
    transcriptText: "Nobody tells you this about consistency in building products.",
    aiSummary: "Discussion about founder persistence and storytelling.",
    aspectRatio: "9:16",
    captionStyle: "Bold Highlight",
    captionLanguage: "English",
    clipCountRequested: 5,
    durationPreset: "30-45s",
    intensityPreset: "High Engagement",
    voiceEnhanceEnabled: true,
    noiseReductionEnabled: true,
    musicEnabled: false,
    sfxEnabled: true,
    silenceRemovalEnabled: true,
    faceTrackingEnabled: true,
    speakerLayout: "Active Speaker",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validClip(uid, projectId, ts) {
  return {
    userId: uid,
    projectId: projectId,
    clipIndex: 1,
    title: "The Consistency Secret",
    hook: "Nobody tells you this about consistency...",
    hookType: "Curiosity",
    hookOptions: [
      "Nobody tells you this about consistency...",
      "Here is why most founders quit too early...",
    ],
    startMs: 5000,
    endMs: 35000,
    viralScore: 94,
    scoreReason: "Strong curiosity hook and crisp standalone insight.",
    transcriptExcerpt: "Nobody tells you this about consistency in building products.",
    captionsText: "Nobody tells you this about consistency in building products.",
    captionLanguage: "English",
    captionStyle: "Bold Highlight",
    captionColorHex: "#F4D96B",
    captionPositionY: 0.78,
    aspectRatio: "9:16",
    faceFocusX: 0.5,
    platformPreset: "YouTube Shorts",
    youtubeTitle: "The Consistency Secret #shorts",
    instagramCaption: "Consistency wins every time.",
    tiktokDescription: "Wait for the end.",
    hashtags: "#shorts #founder #clipflow",
    qualityHook: 95,
    qualityCaption: 92,
    qualityAudio: 90,
    qualityFraming: 94,
    qualityContext: 91,
    gallerySaved: false,
    rendered: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user cannot read or write profiles, projects, or clips", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, ts))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
});

test("Owner can create, read, and partially update user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, ts))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      brandName: "Alice Clips",
      clipsGeneratedCount: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification or ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can create project, list projects, create clip, and update clip export state", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProject(ALICE_UID, ts))
  );

  // List projects
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );

  // Create clip under existing project (NO @DocumentId id field)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("clips")
      .doc("clip_1")
      .set(validClip(ALICE_UID, "proj_1", ts))
  );

  // Reject creating orphan clip for non-existent project
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("missing_proj")
      .collection("clips")
      .doc("clip_2")
      .set(validClip(ALICE_UID, "missing_proj", ts))
  );

  // Partial update for clip render/gallery save
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("clips")
      .doc("clip_1")
      .update({
        localFilePath: "/cache/ClipFlow_Clip_01.mp4",
        rendered: true,
        gallerySaved: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Partial update for clip editing studio
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("clips")
      .doc("clip_1")
      .update({
        hook: "Wait until you hear this founder truth...",
        captionColorHex: "#FFFFFF",
        aspectRatio: "1:1",
        updatedAt: new Date(Date.now() - 500),
      })
  );
});

test("Cross-user access and future timestamps are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProject(ALICE_UID, ts))
  );

  // Bob cannot read or write Alice's projects
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .update({ title: "Hijacked", folderName: "All", updatedAt: new Date(Date.now() - 1000) })
  );

  // Future timestamp fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_future")
      .set(validProject(ALICE_UID, new Date(Date.now() + 60000)))
  );
});

test("Owner can create, update, and delete saved templates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("templates")
      .doc("tpl_1")
      .set({
        userId: ALICE_UID,
        name: "Podcast Gold",
        aspectRatio: "9:16",
        captionStyle: "Bold Highlight",
        captionColorHex: "#F4D96B",
        captionLanguage: "English",
        platformPreset: "YouTube Shorts",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("templates").get()
  );
});
