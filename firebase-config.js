// ═══════════════════════════════════════════════════════════════
//  NDAU DIGITAL — FIREBASE COMPLETO
//  Ficheiro: firebase-config.js
//  Inclui: Config, Auth, Firestore, Storage, Analytics
// ═══════════════════════════════════════════════════════════════

// ── 1. INSTALAÇÃO (Terminal) ───────────────────────────────────
// npm install firebase
// ou via CDN no HTML:
// <script type="module" src="firebase-config.js"></script>

import { initializeApp } from "firebase/app";
import {
  getAuth,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  updateProfile
} from "firebase/auth";
import {
  getFirestore,
  doc, setDoc, getDoc, addDoc, updateDoc, deleteDoc,
  collection, query, where, orderBy, limit,
  onSnapshot, serverTimestamp, increment, getDocs
} from "firebase/firestore";
import {
  getStorage,
  ref, uploadBytes, getDownloadURL
} from "firebase/storage";
import { getAnalytics, logEvent } from "firebase/analytics";

// ── 2. CONFIGURAÇÃO DO PROJECTO ───────────────────────────────
// ⚠️  Substitua com as suas credenciais do Firebase Console
//     https://console.firebase.google.com → Criar Projecto → "ndau-digital"
const firebaseConfig = {
  apiKey:            "SUA_API_KEY_AQUI",
  authDomain:        "ndau-digital.firebaseapp.com",
  projectId:         "ndau-digital",
  storageBucket:     "ndau-digital.appspot.com",
  messagingSenderId: "SEU_SENDER_ID",
  appId:             "SEU_APP_ID",
  measurementId:     "G-XXXXXXXXXX"
};

const app       = initializeApp(firebaseConfig);
const auth      = getAuth(app);
const db        = getFirestore(app);
const storage   = getStorage(app);
const analytics = getAnalytics(app);

// ═══════════════════════════════════════════════════════════════
//  ESTRUTURA DA BASE DE DADOS (Firestore)
// ═══════════════════════════════════════════════════════════════
/*
  COLECÇÕES PRINCIPAIS:

  /users/{uid}
    ├── name:         string
    ├── email:        string
    ├── phone:        string          (para M-Pesa/E-Mola)
    ├── ndauLevel:    "native"|"fluent"|"intermediate"|"learner"
    ├── totalWords:   number
    ├── totalVoice:   number
    ├── pendingMT:    number
    ├── earnedMT:     number
    ├── withdrawnMT:  number
    ├── createdAt:    timestamp
    └── lastActive:   timestamp

  /translations/{id}
    ├── userId:       string
    ├── userName:     string
    ├── sourceLang:   "pt"|"en"|"sn"
    ├── sourceText:   string
    ├── ndauText:     string
    ├── wordCount:    number
    ├── status:       "pending"|"validated"|"rejected"
    ├── earnedMT:     number          (0 até validado)
    ├── validatedBy:  string[]        (uids dos validadores)
    ├── rejectedBy:   string[]
    ├── type:         "text"
    └── createdAt:    timestamp

  /recordings/{id}
    ├── userId:       string
    ├── userName:     string
    ├── word:         string          (palavra Ndau)
    ├── meaning:      string          (significado PT)
    ├── audioUrl:     string          (Firebase Storage URL)
    ├── duration:     number          (segundos)
    ├── status:       "pending"|"validated"|"rejected"
    ├── earnedMT:     number
    ├── validatedBy:  string[]
    ├── type:         "voice"
    └── createdAt:    timestamp

  /withdrawals/{id}
    ├── userId:       string
    ├── amount:       number
    ├── method:       "mpesa"|"emola"|"bank"
    ├── phoneNumber:  string
    ├── status:       "pending"|"processing"|"completed"|"failed"
    ├── reference:    string
    └── createdAt:    timestamp

  /ndau_dictionary/{id}
    ├── ndau:         string
    ├── portuguese:   string
    ├── english:      string
    ├── category:     string
    ├── audioUrl:     string
    ├── validated:    boolean
    ├── addedBy:      string
    └── createdAt:    timestamp

  /platform_stats (documento único)
    ├── totalWords:   number
    ├── totalVoice:   number
    ├── totalUsers:   number
    ├── totalPaidMT:  number
    └── lastUpdated:  timestamp
*/

// ═══════════════════════════════════════════════════════════════
//  AUTENTICAÇÃO
// ═══════════════════════════════════════════════════════════════

export async function registerUser({ name, email, phone, password, ndauLevel }) {
  try {
    const cred = await createUserWithEmailAndPassword(auth, email, password);
    await updateProfile(cred.user, { displayName: name });

    // Criar documento do utilizador no Firestore
    await setDoc(doc(db, "users", cred.user.uid), {
      name,
      email,
      phone,
      ndauLevel,
      totalWords:  0,
      totalVoice:  0,
      pendingMT:   0,
      earnedMT:    0,
      withdrawnMT: 0,
      createdAt:   serverTimestamp(),
      lastActive:  serverTimestamp(),
    });

    // Incrementar contador de utilizadores
    await updateDoc(doc(db, "platform_stats", "global"), {
      totalUsers: increment(1)
    });

    logEvent(analytics, "sign_up", { method: "email" });
    return { success: true, user: cred.user };
  } catch (err) {
    return { success: false, error: err.message };
  }
}

export async function loginUser({ email, password }) {
  try {
    const cred = await signInWithEmailAndPassword(auth, email, password);
    await updateDoc(doc(db, "users", cred.user.uid), {
      lastActive: serverTimestamp()
    });
    logEvent(analytics, "login");
    return { success: true, user: cred.user };
  } catch (err) {
    return { success: false, error: err.message };
  }
}

export async function logoutUser() {
  await signOut(auth);
}

export function onAuthChange(callback) {
  return onAuthStateChanged(auth, callback);
}

export async function getUserProfile(uid) {
  const snap = await getDoc(doc(db, "users", uid));
  return snap.exists() ? { id: snap.id, ...snap.data() } : null;
}

// ═══════════════════════════════════════════════════════════════
//  TRADUÇÕES DE TEXTO
// ═══════════════════════════════════════════════════════════════

export async function submitTranslation({ userId, userName, sourceLang, sourceText, ndauText }) {
  const wordCount = ndauText.trim().split(/\s+/).filter(Boolean).length;

  const ref = await addDoc(collection(db, "translations"), {
    userId,
    userName,
    sourceLang,
    sourceText,
    ndauText,
    wordCount,
    status:      "pending",
    earnedMT:    0,
    validatedBy: [],
    rejectedBy:  [],
    type:        "text",
    createdAt:   serverTimestamp(),
  });

  // Actualizar stats do utilizador (words pendentes)
  await updateDoc(doc(db, "users", userId), {
    totalWords: increment(wordCount),
    pendingMT:  increment(Math.floor(wordCount / 10) * 5),
  });

  logEvent(analytics, "translation_submitted", { wordCount });
  return ref.id;
}

export async function validateTranslation({ translationId, validatorId, action }) {
  const tRef  = doc(db, "translations", translationId);
  const tSnap = await getDoc(tRef);
  if (!tSnap.exists()) return;

  const t = tSnap.data();
  const field = action === "approve" ? "validatedBy" : "rejectedBy";
  const arr   = [...(t[field] || []), validatorId];

  let updates = { [field]: arr };

  // Validado por 2+ pessoas → aprovar e pagar
  if (action === "approve" && arr.length >= 2 && t.status === "pending") {
    const earned = Math.floor(t.wordCount / 10) * 5;
    updates = { ...updates, status: "validated", earnedMT: earned };

    // Mover MT de pendente para ganho
    await updateDoc(doc(db, "users", t.userId), {
      pendingMT: increment(-earned),
      earnedMT:  increment(earned),
    });

    // Stats globais
    await updateDoc(doc(db, "platform_stats", "global"), {
      totalWords:  increment(t.wordCount),
      totalPaidMT: increment(earned),
    });

    logEvent(analytics, "translation_validated", { earned });
  }

  // Rejeitado por 2+ → rejeitar
  if (action === "reject" && arr.length >= 2 && t.status === "pending") {
    const pending = Math.floor(t.wordCount / 10) * 5;
    updates = { ...updates, status: "rejected" };
    await updateDoc(doc(db, "users", t.userId), {
      pendingMT:  increment(-pending),
      totalWords: increment(-t.wordCount),
    });
  }

  await updateDoc(tRef, updates);
}

// ═══════════════════════════════════════════════════════════════
//  GRAVAÇÕES DE VOZ
// ═══════════════════════════════════════════════════════════════

export async function uploadRecording({ userId, userName, word, meaning, audioBlob }) {
  // 1. Upload do áudio para Firebase Storage
  const audioRef  = ref(storage, `recordings/${userId}/${Date.now()}_${word}.webm`);
  const snapshot  = await uploadBytes(audioRef, audioBlob);
  const audioUrl  = await getDownloadURL(snapshot.ref);

  // 2. Guardar metadados no Firestore
  const docRef = await addDoc(collection(db, "recordings"), {
    userId,
    userName,
    word,
    meaning,
    audioUrl,
    duration:    0,
    status:      "pending",
    earnedMT:    0,
    validatedBy: [],
    type:        "voice",
    createdAt:   serverTimestamp(),
  });

  // 3. Actualizar contagem do utilizador
  await updateDoc(doc(db, "users", userId), {
    totalVoice: increment(1),
    pendingMT:  increment(1), // 10 MT / 10 gravações = 1 MT por gravação
  });

  logEvent(analytics, "recording_submitted", { word });
  return { id: docRef.id, audioUrl };
}

export async function validateRecording({ recordingId, validatorId, action }) {
  const rRef  = doc(db, "recordings", recordingId);
  const rSnap = await getDoc(rRef);
  if (!rSnap.exists()) return;

  const r   = rSnap.data();
  const arr = [...(r.validatedBy || []), validatorId];

  if (action === "approve" && arr.length >= 2 && r.status === "pending") {
    const earned = 1; // acumula, 10 MT quando completar 10
    await updateDoc(rRef, { validatedBy: arr, status: "validated", earnedMT: earned });
    await updateDoc(doc(db, "users", r.userId), {
      pendingMT: increment(-earned),
      earnedMT:  increment(earned),
    });
    await updateDoc(doc(db, "platform_stats", "global"), {
      totalVoice:  increment(1),
      totalPaidMT: increment(earned),
    });
  } else {
    await updateDoc(rRef, { [action === "approve" ? "validatedBy" : "rejectedBy"]: arr });
  }
}

// ═══════════════════════════════════════════════════════════════
//  LEVANTAMENTOS / PAGAMENTOS
// ═══════════════════════════════════════════════════════════════

export async function requestWithdrawal({ userId, amount, method, phoneNumber }) {
  const userRef  = doc(db, "users", userId);
  const userSnap = await getDoc(userRef);
  const user     = userSnap.data();

  if (user.earnedMT < amount) {
    return { success: false, error: "Saldo insuficiente" };
  }

  // Criar pedido de levantamento
  const wRef = await addDoc(collection(db, "withdrawals"), {
    userId,
    amount,
    method,   // "mpesa" | "emola" | "bank"
    phoneNumber,
    status:    "pending",
    reference: `NDW-${Date.now()}`,
    createdAt: serverTimestamp(),
  });

  // Deduzir do saldo
  await updateDoc(userRef, {
    earnedMT:    increment(-amount),
    withdrawnMT: increment(amount),
  });

  logEvent(analytics, "withdrawal_requested", { method, amount });
  return { success: true, withdrawalId: wRef.id };
}

// ═══════════════════════════════════════════════════════════════
//  CONSULTAS / QUERIES
// ═══════════════════════════════════════════════════════════════

// Buscar fila de validação (excluindo as do próprio utilizador)
export async function getValidationQueue(currentUserId, limitN = 20) {
  const q = query(
    collection(db, "translations"),
    where("status",  "==", "pending"),
    where("userId",  "!=", currentUserId),
    orderBy("userId"),
    orderBy("createdAt"),
    limit(limitN)
  );
  const snap = await getDocs(q);
  return snap.docs.map(d => ({ id: d.id, ...d.data() }));
}

// Buscar histórico do utilizador
export async function getUserHistory(userId, limitN = 50) {
  const tQuery = query(
    collection(db, "translations"),
    where("userId", "==", userId),
    orderBy("createdAt", "desc"),
    limit(limitN)
  );
  const rQuery = query(
    collection(db, "recordings"),
    where("userId", "==", userId),
    orderBy("createdAt", "desc"),
    limit(limitN)
  );
  const [tSnap, rSnap] = await Promise.all([getDocs(tQuery), getDocs(rQuery)]);
  const translations = tSnap.docs.map(d => ({ id: d.id, ...d.data() }));
  const recordings   = rSnap.docs.map(d => ({ id: d.id, ...d.data() }));
  return [...translations, ...recordings].sort((a, b) =>
    b.createdAt?.seconds - a.createdAt?.seconds
  );
}

// Estatísticas globais em tempo real
export function watchPlatformStats(callback) {
  return onSnapshot(doc(db, "platform_stats", "global"), snap => {
    if (snap.exists()) callback(snap.data());
  });
}

// Perfil do utilizador em tempo real
export function watchUserProfile(uid, callback) {
  return onSnapshot(doc(db, "users", uid), snap => {
    if (snap.exists()) callback({ id: snap.id, ...snap.data() });
  });
}

export { auth, db, storage, analytics };
