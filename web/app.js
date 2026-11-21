import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithPopup,
  signInWithRedirect,
  signOut
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";
import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getFirestore,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  updateDoc
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";
import { firebaseConfig, FIRESTORE_DATABASE_ID } from "./firebase-config.js";

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app, FIRESTORE_DATABASE_ID);

const PARTNER_STORAGE_KEY = "heimboard_active_partner_role";

let activeRole = localStorage.getItem(PARTNER_STORAGE_KEY) || "partnerA";
let householdConfig = {
  partnerA: "Matti",
  partnerB: "Diana",
  cityName: "Berlin",
  latitude: 52.52,
  longitude: 13.405,
  nightModeEnabled: false,
  nightModeStartHour: 22,
  nightModeEndHour: 6
};
let selectedAssignee = "both";
let currentUid = null;
let unsubscribers = [];
let shoppingItemsCache = [];

// DOM References
const authView = document.getElementById("auth-view");
const mainView = document.getElementById("main-view");
const bottomNav = document.getElementById("bottom-nav");
const partnerSwitcher = document.getElementById("partner-switcher");
const btnPartnerA = document.getElementById("btn-partner-a");
const btnPartnerB = document.getElementById("btn-partner-b");
const segPartnerA = document.getElementById("seg-partner-a");
const segPartnerB = document.getElementById("seg-partner-b");
const authError = document.getElementById("auth-error");

function todayIso() {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function activePartnerName() {
  return activeRole === "partnerB" ? householdConfig.partnerB : householdConfig.partnerA;
}

function renderPartnerNames() {
  btnPartnerA.textContent = householdConfig.partnerA;
  btnPartnerB.textContent = householdConfig.partnerB;
  segPartnerA.textContent = householdConfig.partnerA;
  segPartnerB.textContent = householdConfig.partnerB;
  btnPartnerA.classList.toggle("active", activeRole === "partnerA");
  btnPartnerB.classList.toggle("active", activeRole === "partnerB");
}

function setActiveRole(role) {
  activeRole = role === "partnerB" ? "partnerB" : "partnerA";
  localStorage.setItem(PARTNER_STORAGE_KEY, activeRole);
  renderPartnerNames();
}

btnPartnerA.addEventListener("click", () => setActiveRole("partnerA"));
btnPartnerB.addEventListener("click", () => setActiveRole("partnerB"));

// Tab Navigation
document.querySelectorAll(".nav-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    const target = btn.dataset.tab;
    document.querySelectorAll(".nav-btn").forEach((b) => b.classList.toggle("active", b === btn));
    document.querySelectorAll(".tab-panel").forEach((p) => p.classList.toggle("active", p.id === target));
  });
});

// Event assignee segmented buttons
document.querySelectorAll("#event-assignee-group .seg-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    selectedAssignee = btn.dataset.assignee;
    document.querySelectorAll("#event-assignee-group .seg-btn").forEach((b) =>
      b.classList.toggle("active", b === btn)
    );
  });
});

// Default dates for calendar & trash inputs
document.getElementById("input-event-date").value = todayIso();
document.getElementById("input-trash-date").value = todayIso();

// Google Sign-In
document.getElementById("btn-google-signin").addEventListener("click", async () => {
  authError.classList.add("hidden");
  const provider = new GoogleAuthProvider();
  try {
    await signInWithPopup(auth, provider);
  } catch (err) {
    if (err.code === "auth/popup-blocked" || err.code === "auth/operation-not-supported-in-this-environment") {
      await signInWithRedirect(auth, provider);
    } else {
      authError.textContent = err.message;
      authError.classList.remove("hidden");
    }
  }
});

document.getElementById("btn-signout").addEventListener("click", async () => {
  cleanupListeners();
  await signOut(auth);
});

function cleanupListeners() {
  unsubscribers.forEach((unsub) => unsub());
  unsubscribers = [];
}

onAuthStateChanged(auth, async (user) => {
  cleanupListeners();
  if (!user) {
    currentUid = null;
    authView.classList.remove("hidden");
    mainView.classList.add("hidden");
    bottomNav.classList.add("hidden");
    partnerSwitcher.classList.add("hidden");
    return;
  }

  currentUid = user.uid;
  authView.classList.add("hidden");
  mainView.classList.remove("hidden");
  bottomNav.classList.remove("hidden");
  partnerSwitcher.classList.remove("hidden");
  renderPartnerNames();

  await ensureDefaultConfig(currentUid);
  subscribeAll(currentUid);
});

async function ensureDefaultConfig(uid) {
  const ref = doc(db, "users", uid, "config", "household");
  const snap = await getDoc(ref);
  if (!snap.exists()) {
    await setDoc(ref, {
      ...householdConfig,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
  }
}

function subscribeAll(uid) {
  // 1. Household Config
  unsubscribers.push(
    onSnapshot(doc(db, "users", uid, "config", "household"), (snap) => {
      if (snap.exists()) {
        householdConfig = { ...householdConfig, ...snap.data() };
        renderPartnerNames();
        document.getElementById("input-partner-a").value = householdConfig.partnerA;
        document.getElementById("input-partner-b").value = householdConfig.partnerB;
        document.getElementById("input-night-mode").checked = Boolean(householdConfig.nightModeEnabled);
        document.getElementById("selected-city-label").textContent =
          `Aktiver Wetterort: ${householdConfig.cityName} (${householdConfig.latitude.toFixed(2)}, ${householdConfig.longitude.toFixed(2)})`;
      }
    })
  );

  // 2. Shopping List
  unsubscribers.push(
    onSnapshot(query(collection(db, "users", uid, "shopping"), orderBy("createdAt", "desc")), (snap) => {
      shoppingItemsCache = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderShopping(shoppingItemsCache);
    })
  );

  // 3. Todos
  unsubscribers.push(
    onSnapshot(query(collection(db, "users", uid, "todos"), orderBy("createdAt", "desc")), (snap) => {
      const items = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderTodos(items);
    })
  );

  // 4. Calendar Events
  unsubscribers.push(
    onSnapshot(query(collection(db, "users", uid, "events"), orderBy("eventDate", "asc")), (snap) => {
      const items = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderEvents(items);
    })
  );

  // 5. Trash Pickups
  unsubscribers.push(
    onSnapshot(query(collection(db, "users", uid, "trash"), orderBy("pickupDate", "asc")), (snap) => {
      const items = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderTrash(items);
    })
  );

  // 6. Bulletin Board Notes
  unsubscribers.push(
    onSnapshot(query(collection(db, "users", uid, "notes"), orderBy("createdAt", "desc")), (snap) => {
      const items = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderNotes(items);
    })
  );
}

// --- Renderers & Forms ---
function renderShopping(items) {
  const ul = document.getElementById("list-shopping");
  ul.innerHTML = "";
  const sorted = [...items.filter((i) => !i.checked), ...items.filter((i) => i.checked)];
  sorted.forEach((item) => {
    const li = document.createElement("li");
    li.className = "item-row";
    li.innerHTML = `
      <div class="item-main">
        <button type="button" class="check-btn ${item.checked ? "checked" : ""}" aria-label="Abhaken">${item.checked ? "✓" : ""}</button>
        <div>
          <div class="item-title ${item.checked ? "done" : ""}">${escapeHtml(item.quantity ? `${item.quantity} ${item.name}` : item.name)}</div>
          <div class="item-meta">von ${escapeHtml(item.addedBy)}</div>
        </div>
      </div>
      <button type="button" class="delete-btn" aria-label="Löschen">✕</button>
    `;
    li.querySelector(".check-btn").addEventListener("click", () =>
      updateDoc(doc(db, "users", currentUid, "shopping", item.id), {
        checked: !item.checked,
        updatedAt: serverTimestamp()
      })
    );
    li.querySelector(".delete-btn").addEventListener("click", () =>
      deleteDoc(doc(db, "users", currentUid, "shopping", item.id))
    );
    ul.appendChild(li);
  });
}

document.getElementById("form-shopping").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!currentUid) return;
  const nameInput = document.getElementById("input-shopping-name");
  const qtyInput = document.getElementById("input-shopping-qty");
  const name = nameInput.value.trim();
  if (!name) return;
  await addDoc(collection(db, "users", currentUid, "shopping"), {
    name,
    quantity: qtyInput.value.trim(),
    checked: false,
    addedBy: activePartnerName(),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });
  nameInput.value = "";
  qtyInput.value = "";
});

document.getElementById("btn-clear-checked").addEventListener("click", async () => {
  if (!currentUid) return;
  const checked = shoppingItemsCache.filter((i) => i.checked);
  await Promise.all(checked.map((i) => deleteDoc(doc(db, "users", currentUid, "shopping", i.id))));
});

function renderTodos(items) {
  const ul = document.getElementById("list-todos");
  ul.innerHTML = "";
  const today = todayIso();
  const visible = items.filter((t) => !t.completed || t.completedDate === today);
  const sorted = [...visible.filter((t) => !t.completed), ...visible.filter((t) => t.completed)];

  sorted.forEach((todo) => {
    const li = document.createElement("li");
    li.className = "item-row";
    const badge = todo.completed && todo.completedBy
      ? `<span class="badge">${escapeHtml(todo.completedBy)}</span>`
      : "";
    li.innerHTML = `
      <div class="item-main">
        <button type="button" class="check-btn ${todo.completed ? "checked" : ""}" aria-label="Erledigt">${todo.completed ? "✓" : ""}</button>
        <div>
          <div class="item-title ${todo.completed ? "done" : ""}">${escapeHtml(todo.title)}${badge}</div>
          <div class="item-meta">erstellt von ${escapeHtml(todo.createdBy)}</div>
        </div>
      </div>
      <button type="button" class="delete-btn" aria-label="Löschen">✕</button>
    `;
    li.querySelector(".check-btn").addEventListener("click", () => {
      const nextDone = !todo.completed;
      return updateDoc(doc(db, "users", currentUid, "todos", todo.id), {
        completed: nextDone,
        completedBy: nextDone ? activePartnerName() : "",
        completedDate: nextDone ? todayIso() : "",
        updatedAt: serverTimestamp()
      });
    });
    li.querySelector(".delete-btn").addEventListener("click", () =>
      deleteDoc(doc(db, "users", currentUid, "todos", todo.id))
    );
    ul.appendChild(li);
  });
}

document.getElementById("form-todo").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!currentUid) return;
  const input = document.getElementById("input-todo-title");
  const title = input.value.trim();
  if (!title) return;
  await addDoc(collection(db, "users", currentUid, "todos"), {
    title,
    completed: false,
    completedBy: "",
    completedDate: "",
    createdBy: activePartnerName(),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });
  input.value = "";
});

function renderEvents(items) {
  const ul = document.getElementById("list-events");
  ul.innerHTML = "";
  const today = todayIso();
  items.filter((e) => e.eventDate >= today).forEach((evt) => {
    const who = evt.assignee === "partnerA"
      ? householdConfig.partnerA
      : evt.assignee === "partnerB"
        ? householdConfig.partnerB
        : "Beide";
    const li = document.createElement("li");
    li.className = "item-row";
    li.innerHTML = `
      <div class="item-main">
        <div>
          <div class="item-title">${escapeHtml(evt.title)} <span class="badge">${escapeHtml(who)}</span></div>
          <div class="item-meta">${escapeHtml(evt.eventDate)}${evt.eventTime ? ` · ${escapeHtml(evt.eventTime)} Uhr` : ""}</div>
        </div>
      </div>
      <button type="button" class="delete-btn" aria-label="Löschen">✕</button>
    `;
    li.querySelector(".delete-btn").addEventListener("click", () =>
      deleteDoc(doc(db, "users", currentUid, "events", evt.id))
    );
    ul.appendChild(li);
  });
}

document.getElementById("form-event").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!currentUid) return;
  const titleInput = document.getElementById("input-event-title");
  const dateInput = document.getElementById("input-event-date");
  const timeInput = document.getElementById("input-event-time");
  const title = titleInput.value.trim();
  if (!title || !dateInput.value) return;
  await addDoc(collection(db, "users", currentUid, "events"), {
    title,
    eventDate: dateInput.value,
    eventTime: timeInput.value || "",
    assignee: selectedAssignee,
    createdBy: activePartnerName(),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });
  titleInput.value = "";
  timeInput.value = "";
});

const WASTE_LABELS = {
  restmuell: "Restmüll",
  gelber_sack: "Gelber Sack",
  papier: "Papier",
  bio: "Biomüll"
};

function renderTrash(items) {
  const ul = document.getElementById("list-trash");
  ul.innerHTML = "";
  const today = todayIso();
  items.filter((t) => t.pickupDate >= today).forEach((pickup) => {
    const li = document.createElement("li");
    li.className = "item-row";
    const label = WASTE_LABELS[pickup.wasteType] || pickup.wasteType;
    li.innerHTML = `
      <div class="item-main">
        <div>
          <div class="item-title">
            <span class="badge badge-${escapeHtml(pickup.wasteType)}">${escapeHtml(label)}</span>
            ${escapeHtml(pickup.pickupDate)}
          </div>
          <div class="item-meta">${pickup.putOut ? `Rausgestellt (${escapeHtml(pickup.putOutBy)})` : "Noch nicht rausgestellt"}</div>
        </div>
      </div>
      <div style="display:flex;gap:6px;align-items:center;">
        <button type="button" class="btn ${pickup.putOut ? "btn-secondary" : "btn-olive"} btn-put-out" style="min-height:38px;padding:6px 12px;font-size:0.82rem;">
          ${pickup.putOut ? "Rückgängig" : "Müll rausgestellt"}
        </button>
        <button type="button" class="delete-btn" aria-label="Löschen">✕</button>
      </div>
    `;
    li.querySelector(".btn-put-out").addEventListener("click", () => {
      const nextOut = !pickup.putOut;
      return updateDoc(doc(db, "users", currentUid, "trash", pickup.id), {
        putOut: nextOut,
        putOutBy: nextOut ? activePartnerName() : "",
        updatedAt: serverTimestamp()
      });
    });
    li.querySelector(".delete-btn").addEventListener("click", () =>
      deleteDoc(doc(db, "users", currentUid, "trash", pickup.id))
    );
    ul.appendChild(li);
  });
}

document.getElementById("form-trash").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!currentUid) return;
  const wasteType = document.getElementById("input-trash-type").value;
  const pickupDate = document.getElementById("input-trash-date").value;
  if (!pickupDate) return;
  await addDoc(collection(db, "users", currentUid, "trash"), {
    wasteType,
    pickupDate,
    putOut: false,
    putOutBy: "",
    createdBy: activePartnerName(),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });
});

function renderNotes(items) {
  const ul = document.getElementById("list-notes");
  ul.innerHTML = "";
  items.forEach((note) => {
    const li = document.createElement("li");
    li.className = "item-row";
    li.innerHTML = `
      <div class="item-main">
        <div>
          <div class="item-title">${escapeHtml(note.message)}</div>
          <div class="item-meta">— ${escapeHtml(note.author)}</div>
        </div>
      </div>
      <button type="button" class="delete-btn" aria-label="Löschen">✕</button>
    `;
    li.querySelector(".delete-btn").addEventListener("click", () =>
      deleteDoc(doc(db, "users", currentUid, "notes", note.id))
    );
    ul.appendChild(li);
  });
}

document.getElementById("form-note").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!currentUid) return;
  const textarea = document.getElementById("input-note-message");
  const message = textarea.value.trim();
  if (!message) return;
  await addDoc(collection(db, "users", currentUid, "notes"), {
    message,
    author: activePartnerName(),
    pinned: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });
  textarea.value = "";
});

// City search via Open-Meteo Geocoding API
document.getElementById("btn-city-search").addEventListener("click", async () => {
  const queryText = document.getElementById("input-city-search").value.trim();
  const container = document.getElementById("city-results");
  if (!queryText) return;
  const res = await fetch(
    `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(queryText)}&count=5&language=de`
  );
  const data = await res.json();
  container.innerHTML = "";
  if (!data.results || data.results.length === 0) {
    container.innerHTML = `<span class="muted small">Kein Ort gefunden.</span>`;
    container.classList.remove("hidden");
    return;
  }
  data.results.forEach((place) => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "city-option";
    const region = [place.admin1, place.country].filter(Boolean).join(", ");
    btn.textContent = `${place.name} (${region})`;
    btn.addEventListener("click", () => {
      householdConfig.cityName = place.name;
      householdConfig.latitude = place.latitude;
      householdConfig.longitude = place.longitude;
      document.getElementById("selected-city-label").textContent =
        `Ausgewählt: ${place.name} (${place.latitude.toFixed(2)}, ${place.longitude.toFixed(2)})`;
      container.classList.add("hidden");
    });
    container.appendChild(btn);
  });
  container.classList.remove("hidden");
});

document.getElementById("form-settings").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!currentUid) return;
  const partnerA = document.getElementById("input-partner-a").value.trim() || "Matti";
  const partnerB = document.getElementById("input-partner-b").value.trim() || "Diana";
  const nightModeEnabled = document.getElementById("input-night-mode").checked;

  await updateDoc(doc(db, "users", currentUid, "config", "household"), {
    partnerA,
    partnerB,
    cityName: householdConfig.cityName,
    latitude: householdConfig.latitude,
    longitude: householdConfig.longitude,
    nightModeEnabled,
    nightModeStartHour: householdConfig.nightModeStartHour || 22,
    nightModeEndHour: householdConfig.nightModeEndHour || 6,
    updatedAt: serverTimestamp()
  });

  const feedback = document.getElementById("settings-feedback");
  feedback.textContent = "Gespeichert & mit dem TV synchronisiert!";
  feedback.classList.remove("hidden");
  setTimeout(() => feedback.classList.add("hidden"), 3000);
});

function escapeHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
