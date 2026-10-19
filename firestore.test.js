const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_user_1";
const BOB_UID = "bob_user_2";
const CHARLIE_UID = "charlie_rider_3";
const ADMIN_UID = "admin_user_0";
const ADMIN_EMAIL = "mwaqasbhatti1230@gmail.com";

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

// 1. Order Creation by Customer
test("Customer can create a Pending order", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("orders").doc("order_1").set({
    customerId: ALICE_UID,
    assignedRiderId: "",
    itemDescription: "2 cartons of milk, bread from Naan shop",
    customerPhone: "03001234567",
    deliveryBlock: "Block A",
    deliveryStreet: "Street 4",
    deliveryHouseNumber: "House 12",
    orderStatus: "Pending",
    billAmount: 0.0,
    deliveryDistanceKm: 2.0,
    deliveryCharge: 140.0,
    serviceCharge: 0.0,
    platformFee: 4.0,
    totalAmount: 144.0,
    createdAt: new Date(),
  }));
});

// 2. Rider Online Toggle & Order Claiming
test("Approved rider can toggle online status to true and false", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("riders").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Bob Rider",
      phone: "03001234567",
      email: "bob@example.com",
      status: "approved",
      isOnline: false,
      preferredLanguage: "en",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("riders").doc(BOB_UID).update({
    isOnline: true,
    updatedAt: new Date(),
  }));
  await assertSucceeds(bobDb.collection("riders").doc(BOB_UID).update({
    isOnline: false,
    updatedAt: new Date(),
  }));
});

test("Rider can claim an unassigned Pending order", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("riders").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Bob Rider",
      phone: "03001234567",
      email: "bob@example.com",
      status: "approved",
      isOnline: true,
      preferredLanguage: "en",
      createdAt: new Date(),
    });
    await context.firestore().collection("orders").doc("order_1").set({
      customerId: ALICE_UID,
      assignedRiderId: "",
      itemDescription: "Medicine from pharmacy",
      customerPhone: "03001234567",
      deliveryBlock: "Block B",
      deliveryStreet: "Street 2",
      deliveryHouseNumber: "House 8",
      orderStatus: "Pending",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("orders").doc("order_1").update({
    assignedRiderId: BOB_UID,
    orderStatus: "Assigned",
    riderAcceptedAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Approved rider can query unassigned Pending orders", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("orders").doc("order_pending_1").set({
      customerId: ALICE_UID,
      assignedRiderId: "",
      itemDescription: "Groceries for query test",
      customerPhone: "03001234567",
      deliveryBlock: "Block A",
      deliveryStreet: "Street 1",
      deliveryHouseNumber: "House 1",
      orderStatus: "Pending",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const snapshot = await assertSucceeds(bobDb.collection("orders")
    .where("orderStatus", "==", "Pending")
    .where("assignedRiderId", "==", "")
    .get());
  const found = snapshot.docs.some(d => d.id === "order_pending_1");
  if (!found) throw new Error("Expected to find order_pending_1 in unassigned orders query");
});

// 3. Competing Rider Collision Test
test("Second rider cannot claim an already assigned order", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("orders").doc("order_1").set({
      customerId: ALICE_UID,
      assignedRiderId: BOB_UID,
      itemDescription: "Groceries",
      customerPhone: "03001234567",
      deliveryBlock: "Block A",
      deliveryStreet: "Street 1",
      deliveryHouseNumber: "House 1",
      orderStatus: "Assigned",
      createdAt: new Date(),
    });
  });

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("orders").doc("order_1").update({
    assignedRiderId: CHARLIE_UID,
    orderStatus: "Assigned",
  }));
});

// 4. Assigned Rider Submits Bill
test("Assigned rider can submit bill", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("orders").doc("order_1").set({
      customerId: ALICE_UID,
      assignedRiderId: BOB_UID,
      itemDescription: "Groceries",
      customerPhone: "03001234567",
      deliveryBlock: "Block A",
      deliveryStreet: "Street 1",
      deliveryHouseNumber: "House 1",
      orderStatus: "Shopping",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("orders").doc("order_1").update({
    orderStatus: "Awaiting Customer Bill Approval",
    billApprovalStatus: "pending_approval",
    billAmount: 500.0,
    serviceCharge: 25.0,
    deliveryDistanceKm: 2.0,
    deliveryCharge: 140.0,
    platformFee: 4.0,
    totalAmount: 669.0,
    updatedAt: new Date(),
  }));
});

// 5. Customer Approves Bill
test("Customer can approve bill", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("orders").doc("order_1").set({
      customerId: ALICE_UID,
      assignedRiderId: BOB_UID,
      itemDescription: "Groceries",
      customerPhone: "03001234567",
      deliveryBlock: "Block A",
      deliveryStreet: "Street 1",
      deliveryHouseNumber: "House 1",
      orderStatus: "Awaiting Customer Bill Approval",
      billApprovalStatus: "pending_approval",
      billAmount: 500.0,
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("orders").doc("order_1").update({
    orderStatus: "Bill Approved",
    billApprovalStatus: "approved",
    updatedAt: new Date(),
  }));
});

// 6. Assigned Rider Starts Delivery & Marks Delivered
test("Assigned rider can start delivery journey and mark delivered", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("orders").doc("order_deliv").set({
      customerId: ALICE_UID,
      assignedRiderId: BOB_UID,
      itemDescription: "Delivery test",
      customerPhone: "03001234567",
      deliveryBlock: "Block A",
      deliveryStreet: "Street 1",
      deliveryHouseNumber: "House 1",
      orderStatus: "Bill Approved",
      billApprovalStatus: "approved",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("orders").doc("order_deliv").update({
    orderStatus: "Out for Delivery",
    deliveryStartedAt: new Date(),
    updatedAt: new Date(),
  }));

  await assertSucceeds(bobDb.collection("orders").doc("order_deliv").update({
    orderStatus: "Delivered",
    deliveredAt: new Date(),
    updatedAt: new Date(),
  }));
});

// 7. Security Rejection: Unapproved User Cannot Claim Order
test("Unapproved user cannot claim an unassigned order", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("orders").doc("order_claim_test").set({
      customerId: ALICE_UID,
      assignedRiderId: "",
      itemDescription: "Groceries",
      customerPhone: "03001234567",
      deliveryBlock: "Block A",
      deliveryStreet: "Street 1",
      deliveryHouseNumber: "House 1",
      orderStatus: "Pending",
      createdAt: new Date(),
    });
  });

  // CHARLIE_UID has no approved rider document in /riders
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("orders").doc("order_claim_test").update({
    assignedRiderId: CHARLIE_UID,
    orderStatus: "Assigned",
    riderAcceptedAt: new Date(),
    updatedAt: new Date(),
  }));
});

// 8. Cross-Customer Protection: Customer Cannot Modify Another Customer's Order
test("Customer cannot modify an order belonging to another customer", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("orders").doc("order_alice").set({
      customerId: ALICE_UID,
      assignedRiderId: "",
      itemDescription: "Private items",
      customerPhone: "03001234567",
      deliveryBlock: "Block A",
      deliveryStreet: "Street 1",
      deliveryHouseNumber: "House 1",
      orderStatus: "Pending",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("orders").doc("order_alice").update({
    itemDescription: "Hacked item description",
  }));
});

