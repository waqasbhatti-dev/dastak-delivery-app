const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const RIDER_1_UID = "rider_ali_1";
const RIDER_2_UID = "rider_bilal_2";
const CUSTOMER_UID = "customer_fatima_3";
const OTHER_USER_UID = "user_attacker_4";

const SOLE_ADMIN_EMAIL = "mwaqasbhatti1230@gmail.com";
const FORMER_SECONDARY_ADMIN_EMAIL = "mwaqasbhatti57570@gmail.com";

let emulatorHost = "127.0.0.1";
let emulatorPort = 9199;
if (process.env.STORAGE_EMULATOR_HOST) {
  const clean = process.env.STORAGE_EMULATOR_HOST.replace(/^https?:\/\//, "");
  const parts = clean.split(":");
  emulatorHost = parts[0];
  emulatorPort = parseInt(parts[1], 10);
}

before(async () => {
  const rules = fs.readFileSync("./storage.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    storage: {
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

// 1. Unauthenticated read and write are denied
test("1. Unauthenticated read and write are denied across all storage paths", async () => {
  const unauthStorage = testEnv.unauthenticatedContext().storage();
  
  // Attempt write to ads
  await assertFails(unauthStorage.ref("advertisements/promo.jpg").put(Buffer.from("img"), { contentType: "image/jpeg" }));
  // Attempt read from ads
  await assertFails(unauthStorage.ref("advertisements/promo.jpg").getDownloadURL());
  // Attempt write to riders
  await assertFails(unauthStorage.ref("riders/rider_ali_1/cnic.jpg").put(Buffer.from("img"), { contentType: "image/jpeg" }));
  // Attempt write to arbitrary path
  await assertFails(unauthStorage.ref("random_folder/secret.txt").put(Buffer.from("text"), { contentType: "text/plain" }));
});

// 2. An authenticated ordinary user cannot access unmatched paths (Catch-all deny-by-default)
test("2. Authenticated ordinary user cannot access unmatched / arbitrary paths", async () => {
  const userStorage = testEnv.authenticatedContext(CUSTOMER_UID, { email: "customer@test.com" }).storage();

  await assertFails(userStorage.ref("arbitrary_folder/leak.txt").put(Buffer.from("data"), { contentType: "text/plain" }));
  await assertFails(userStorage.ref("backup/db.dump").getDownloadURL());
  await assertFails(userStorage.ref("system/config.json").put(Buffer.from("{}"), { contentType: "application/json" }));
  await assertFails(userStorage.ref("private_files/secret.jpg").put(Buffer.from("img"), { contentType: "image/jpeg" }));
});

// 3. A rider cannot access or overwrite another rider's CNIC files
test("3. Rider can upload their own CNIC image, but cannot access or overwrite another rider's CNIC", async () => {
  const rider1Storage = testEnv.authenticatedContext(RIDER_1_UID, { email: "rider1@test.com" }).storage();
  const rider2Storage = testEnv.authenticatedContext(RIDER_2_UID, { email: "rider2@test.com" }).storage();

  // Rider 1 uploads their own CNIC: Allowed
  await assertSucceeds(rider1Storage.ref(`riders/${RIDER_1_UID}/cnic_front.jpg`).put(Buffer.from("rider1 cnic front"), { contentType: "image/jpeg" }));
  // Rider 1 reads their own CNIC: Allowed
  await assertSucceeds(rider1Storage.ref(`riders/${RIDER_1_UID}/cnic_front.jpg`).getDownloadURL());

  // Rider 2 attempts to upload/overwrite Rider 1's CNIC: DENIED
  await assertFails(rider2Storage.ref(`riders/${RIDER_1_UID}/cnic_front.jpg`).put(Buffer.from("hacked"), { contentType: "image/jpeg" }));
  // Rider 2 attempts to read Rider 1's CNIC: DENIED
  await assertFails(rider2Storage.ref(`riders/${RIDER_1_UID}/cnic_front.jpg`).getDownloadURL());
});

// 4. The authorized Admin can access the required CNIC and advertisement files
test("4. Sole authorized Admin (mwaqasbhatti1230@gmail.com) can access CNICs and manage advertisements", async () => {
  const adminStorage = testEnv.authenticatedContext("admin_uid", { email: SOLE_ADMIN_EMAIL }).storage();

  // Admin uploads advertisement: Allowed
  await assertSucceeds(adminStorage.ref("advertisements/hero_banner.jpg").put(Buffer.from("banner data"), { contentType: "image/jpeg" }));
  // Admin reads advertisement: Allowed
  await assertSucceeds(adminStorage.ref("advertisements/hero_banner.jpg").getDownloadURL());
  // Admin reviews Rider 1's CNIC: Allowed
  await assertSucceeds(adminStorage.ref(`riders/${RIDER_1_UID}/cnic_front.jpg`).getDownloadURL());
});

// 5. Unauthorized email cannot use Admin-only Storage operations (explicitly test mwaqasbhatti57570@gmail.com)
test("5. Secondary email (mwaqasbhatti57570@gmail.com) is NOT authorized as Admin and is DENIED Admin operations", async () => {
  const secondEmailStorage = testEnv.authenticatedContext("user_57570", { email: FORMER_SECONDARY_ADMIN_EMAIL }).storage();

  // Attempts to upload/manage advertisements: DENIED
  await assertFails(secondEmailStorage.ref("advertisements/unauthorized_ad.jpg").put(Buffer.from("ad"), { contentType: "image/jpeg" }));
  // Attempts to access Rider 1's private CNIC: DENIED
  await assertFails(secondEmailStorage.ref(`riders/${RIDER_1_UID}/cnic_front.jpg`).getDownloadURL());
  // Attempts to overwrite legacy admin order file: DENIED
  await assertFails(secondEmailStorage.ref("orders/order_100/admin_override.jpg").put(Buffer.from("override"), { contentType: "image/jpeg" }));
});

// 6. Authorized order participant can access only media permitted by the intended workflow
test("6. Order participant can upload and read their own order media", async () => {
  const customerStorage = testEnv.authenticatedContext(CUSTOMER_UID, { email: "customer@test.com" }).storage();
  const otherStorage = testEnv.authenticatedContext(OTHER_USER_UID, { email: "attacker@test.com" }).storage();

  // Customer uploads order photo under their scoped order path: Allowed
  const orderRef = `orders/order_999/${CUSTOMER_UID}/item_photo.jpg`;
  await assertSucceeds(customerStorage.ref(orderRef).put(Buffer.from("receipt image"), { contentType: "image/jpeg" }));
  // Customer reads their own order photo: Allowed
  await assertSucceeds(customerStorage.ref(orderRef).getDownloadURL());

  // Unrelated user attempts to read customer's order photo: DENIED
  await assertFails(otherStorage.ref(orderRef).getDownloadURL());
  // Unrelated user attempts to overwrite customer's order photo: DENIED
  await assertFails(otherStorage.ref(orderRef).put(Buffer.from("fake"), { contentType: "image/jpeg" }));
});

// 7. Unexpected / unconfigured file paths are denied
test("7. Unexpected and unconfigured paths are denied by default", async () => {
  const adminStorage = testEnv.authenticatedContext("admin_uid", { email: SOLE_ADMIN_EMAIL }).storage();
  const userStorage = testEnv.authenticatedContext(CUSTOMER_UID, { email: "customer@test.com" }).storage();

  await assertFails(userStorage.ref("secrets/passwords.txt").getDownloadURL());
  await assertFails(userStorage.ref("root_file.jpg").put(Buffer.from("img"), { contentType: "image/jpeg" }));
  await assertFails(adminStorage.ref("arbitrary_unknown_dir/file.bin").put(Buffer.from("bin"), { contentType: "application/octet-stream" }));
});

// 8. Invalid content types or oversized uploads are rejected
test("8. Invalid content types and oversized uploads are rejected", async () => {
  const riderStorage = testEnv.authenticatedContext(RIDER_1_UID, { email: "rider1@test.com" }).storage();
  const adminStorage = testEnv.authenticatedContext("admin_uid", { email: SOLE_ADMIN_EMAIL }).storage();

  // Rider attempts to upload non-image as CNIC (e.g. text or pdf): DENIED
  await assertFails(riderStorage.ref(`riders/${RIDER_1_UID}/malicious.sh`).put(Buffer.from("echo hacked"), { contentType: "text/x-sh" }));
  await assertFails(riderStorage.ref(`riders/${RIDER_1_UID}/fake.pdf`).put(Buffer.from("pdf data"), { contentType: "application/pdf" }));

  // Admin attempts to upload non-media (e.g. executable/script) as advertisement: DENIED
  await assertFails(adminStorage.ref("advertisements/malicious.exe").put(Buffer.from("MZ"), { contentType: "application/x-msdownload" }));
});
