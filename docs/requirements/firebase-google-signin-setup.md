# Firebase Google Sign-In setup (Credential Manager)

Use this once per project/environment.

## 1. Firebase + Google Cloud
1. In Firebase Console, open your Android app under this project.
2. In Google Cloud Console > APIs & Services > Credentials, ensure an OAuth 2.0 **Web client** exists.
3. Ensure Google provider is enabled in Firebase Auth > Sign-in method.
4. Download latest `google-services.json` and place it at:
   - `app/google-services.json`

## 2. SHA fingerprints (required)
Register both debug and release signing certificates on the Firebase Android app:
1. Add debug SHA-1/SHA-256.
2. Add release SHA-1/SHA-256 (Play/App Signing key if publishing through Play).
3. Re-download `google-services.json` after adding fingerprints.

Commands to print fingerprints:
```powershell
./gradlew signingReport
```

## 3. Web client ID
- Credential Manager Google sign-in must use the **Web OAuth client ID**.
- In this app code, it is read from:
  - `R.string.default_web_client_id`
- This value is generated from `google-services.json`.

## 4. Runtime behavior
- Login screen uses Credential Manager "Sign in with Google".
- Returned Google ID token is exchanged with Firebase via:
  - `FirebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))`
- After Firebase sign-in, app validates Firestore entitlement/device registration gate before opening core nav.

## 5. Quick failure checks
- If Google sign-in sheet does not appear:
  - Verify Google Play Services on device.
  - Verify `default_web_client_id` is present in generated resources.
  - Verify SHA fingerprints match the currently installed signing key.
- If Firebase auth fails after account selection:
  - Verify Google sign-in provider is enabled.
  - Verify the Web client ID is from the same Firebase project.
