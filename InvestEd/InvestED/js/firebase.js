import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyAUep4TvqaSX6YfgR_bGgt3b1nucaDnv4g",
  authDomain: "invested-163a2.firebaseapp.com",
  projectId: "invested-163a2",
  storageBucket: "invested-163a2.firebasestorage.app",
  messagingSenderId: "279950412673",
  appId: "1:279950412673:web:2eef8f0484007d19907680",
  measurementId: "G-SXSTHTCQ0X"
};

const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app);
