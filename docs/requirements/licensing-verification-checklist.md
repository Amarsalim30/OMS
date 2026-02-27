# Licensing P0 verification checklist

Use this checklist after each licensing/auth change.

- [ ] 1. Debug build Google Sign-In works:
  - Steps: run debug build on device with Google Play Services -> tap "Sign in with Google" -> pick account.
  - Expected: Firebase sign-in succeeds and app opens for authorized user.

- [ ] 2. Release build Google Sign-In works:
  - Steps: install release-signed APK/AAB (production signing key SHA-1/SHA-256 configured) -> sign in with Google.
  - Expected: Firebase sign-in succeeds for authorized user.

- [ ] 3. Unauthorized user is blocked:
  - Steps: set `users/{uid}.allowed=false` for signed-in user -> retry validation.
  - Expected: blocked screen shows not-approved reason.

- [ ] 4. Device limit enforced (`maxDevices=1`):
  - Steps: sign in user on device A (registers device) -> sign in same user on device B.
  - Expected: device B is blocked with "Device limit reached".

- [ ] 5. Revoked device blocked:
  - Steps: mark `users/{uid}/devices/{deviceId}.revoked=true` -> retry validation.
  - Expected: blocked screen shows revoked-device reason.

- [ ] 6. Offline grace works:
  - Steps: sign in successfully online -> disable internet before 3 days elapse -> relaunch.
  - Expected: app opens.

- [ ] 7. Offline beyond grace window blocks:
  - Steps: sign in successfully online -> keep device offline >3 days -> relaunch.
  - Expected: blocked screen asks user to connect internet for validation.
