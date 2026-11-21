// Firebase-Konfiguration für Heimboard (identisch mit Android TV App)
export const firebaseConfig = {
  apiKey: "AIzaSyA-68xG7HgGZ3Ixa4gzK0LABUedkYFBThg",
  authDomain: "com-aistudio-heimboard-f-4b456.firebaseapp.com",
  projectId: "com-aistudio-heimboard-f-4b456",
  storageBucket: "com-aistudio-heimboard-f-4b456.firebasestorage.app",
  messagingSenderId: "164342324055",
  // Hinweis: Falls du in der Firebase Console eine Web-App registrierst, kannst du appId hier ergänzen:
  appId: "1:164342324055:web:heimboard"
};

// Wichtig: Die Android TV App und die Web-App nutzen dieselbe benannte Firestore-Datenbank
export const FIRESTORE_DATABASE_ID =
  "ai-studio-android-heimboar-8e039f9d-6b2c-45bd-98c0-3e8316abecb3";
