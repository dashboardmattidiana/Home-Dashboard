const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_household_123";
const BOB_UID = "bob_stranger_456";

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

test("Unauthenticated users cannot read or write any household collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/events`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/shopping`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/todos`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/trash`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/notes`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/config/household`).set({
      partnerA: "Matti",
      partnerB: "Diana",
      cityName: "Berlin",
      latitude: 52.52,
      longitude: 13.405,
      nightModeEnabled: false,
      nightModeStartHour: 22,
      nightModeEndHour: 6,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's household data", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`users/${ALICE_UID}/notes/note_1`).set({
      message: "Blumen gießen",
      author: "Matti",
      pinned: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/notes/note_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/notes`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/notes/note_1`).update({
      pinned: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/notes/note_1`).delete());
});

test("HouseholdConfig create, partial night mode toggle, and rejection of bad config IDs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/config/household`).set({
      partnerA: "Matti",
      partnerB: "Diana",
      cityName: "Hamburg",
      latitude: 53.5511,
      longitude: 9.9937,
      nightModeEnabled: false,
      nightModeStartHour: 22,
      nightModeEndHour: 6,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update: toggle night mode
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/config/household`).update({
      nightModeEnabled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject wrong doc ID
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/config/other`).set({
      partnerA: "Matti",
      partnerB: "Diana",
      cityName: "Hamburg",
      latitude: 53.5511,
      longitude: 9.9937,
      nightModeEnabled: false,
      nightModeStartHour: 22,
      nightModeEndHour: 6,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject deleting household config
  await assertFails(aliceDb.doc(`users/${ALICE_UID}/config/household`).delete());
});

test("Todos: create, complete with partner badge, reject extra @DocumentId field and createdAt tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/todos/todo_1`).set({
      title: "Spülmaschine ausräumen",
      completed: false,
      completedBy: "",
      completedDate: "",
      createdBy: "Matti",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/todos/todo_1`).update({
      completed: true,
      completedBy: "Diana",
      completedDate: "2026-11-20",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject tampering createdAt
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/todos/todo_1`).update({
      title: "Geändert",
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId 'id' inside payload
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/todos/todo_2`).set({
      id: "todo_2",
      title: "Ungültig",
      completed: false,
      completedBy: "",
      completedDate: "",
      createdBy: "Matti",
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("TrashPickup, CalendarEvent, ShoppingItem, and BoardNote CRUD & validation bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // TrashPickup create + put out toggle
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/trash/trash_1`).set({
      wasteType: "gelber_sack",
      pickupDate: "2026-11-21",
      putOut: false,
      putOutBy: "",
      createdBy: "Matti",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/trash/trash_1`).update({
      putOut: true,
      putOutBy: "Diana",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Reject invalid wasteType
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/trash/trash_2`).set({
      wasteType: "sondermuell",
      pickupDate: "2026-11-21",
      putOut: false,
      putOutBy: "",
      createdBy: "Matti",
      createdAt: created,
      updatedAt: created,
    })
  );

  // CalendarEvent create & list
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/events/evt_1`).set({
      title: "Zahnarzt",
      eventDate: "2026-11-22",
      eventTime: "10:30",
      assignee: "partnerB",
      createdBy: "Diana",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/events`).get());

  // ShoppingItem create & toggle
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/shopping/shop_1`).set({
      name: "Hafermilch",
      quantity: "2x",
      checked: false,
      addedBy: "Matti",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/shopping/shop_1`).update({
      checked: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // BoardNote create & list
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/notes/note_1`).set({
      message: "Paket liegt beim Nachbarn!",
      author: "Diana",
      pinned: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/notes`).get());

  // Reject future timestamp
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/notes/note_future`).set({
      message: "Future spoof",
      author: "Matti",
      pinned: true,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
