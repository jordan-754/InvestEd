const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: functions.config().email.user,
    pass: functions.config().email.pass,
  },
});

// Generate and send OTP
exports.sendOtp = functions.https.onCall(async (data, context) => {
  const email = data.email;

  if (!email) {
    throw new functions.https.HttpsError("invalid-argument", "Email is required");
  }

  // Generate 6-digit OTP
  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  const expiresAt = Date.now() + 5 * 60 * 1000; // 5 minutes

  // Save OTP to Firestore
  await admin.firestore().collection("otps").doc(email).set({
    otp,
    expiresAt,
    used: false,
  });

  // Send email
  await transporter.sendMail({
    from: `"InvestEd" <${functions.config().email.user}>`,
    to: email,
    subject: "Your InvestEd OTP Code",
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 400px; margin: auto;">
        <h2 style="color: #1B3A6B;">InvestEd Verification</h2>
        <p>Your One-Time Password is:</p>
        <h1 style="letter-spacing: 8px; color: #1B3A6B;">${otp}</h1>
        <p>This code expires in <strong>5 minutes</strong>.</p>
        <p style="color: gray; font-size: 12px;">If you didn't request this, ignore this email.</p>
      </div>
    `,
  });

  return { success: true };
});

// Verify OTP
exports.verifyOtp = functions.https.onCall(async (data, context) => {
  const { email, otp } = data;

  if (!email || !otp) {
    throw new functions.https.HttpsError("invalid-argument", "Email and OTP are required");
  }

  const doc = await admin.firestore().collection("otps").doc(email).get();

  if (!doc.exists) {
    throw new functions.https.HttpsError("not-found", "OTP not found. Please request a new one.");
  }

  const { otp: savedOtp, expiresAt, used } = doc.data();

  if (used) {
    throw new functions.https.HttpsError("already-exists", "OTP has already been used.");
  }

  if (Date.now() > expiresAt) {
    throw new functions.https.HttpsError("deadline-exceeded", "OTP has expired. Please request a new one.");
  }

  if (otp !== savedOtp) {
    throw new functions.https.HttpsError("unauthenticated", "Invalid OTP. Please try again.");
  }

  // Mark OTP as used
  await admin.firestore().collection("otps").doc(email).update({ used: true });

  return { success: true };
});
```

After saving the file, go back to the terminal and run:
```
firebase deploy --only functions