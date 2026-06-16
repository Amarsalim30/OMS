# Goal: Simple APK redistribution deterrence (no custom backend)

## Context
This app may be shared privately (APK installs). I want to reduce unauthorized redistribution:
- If someone copies the APK to another device, it should not function without authorization.
- I will be the admin who issues/approves users.
- Target scale: ~100 users.
- Priority: low maintenance + simplicity + minimal cost.
- Avoid building a custom backend/server.

## Non-goals
- Perfect DRM is not required (impossible offline).
- No heavy "dynamic code loading" solutions.
- No IMEI-based device binding.

## Requirements (P0)
1. App must require sign-in to use core features.
2. Authorization must be server-validated:
   - A signed-in user must have `allowed=true` in a server-controlled store.
3. Device binding:
   - Each user may still have `maxDevices` and device records for admin tracking.
   - First run on a device should attempt to register that device.
   - Current runtime policy is entitlement-first: if `allowed=true`, device registration/heartbeat failures must not block app access or surface a fatal validation error.
4. Admin control:
   - Admin can allow/deny users.
   - Admin can revoke a specific device for a user.
5. Minimal infrastructure:
   - Use Firebase Auth.
   - Google Sign-In via Credential Manager remains the primary sign-in option.
   - Email/password sign-in may be enabled as an explicit alternate sign-in option when requested.
   - Use Firestore for entitlements/device registry.
   - No Cloud Functions unless absolutely necessary.
6. Security rules:
   - Users can read their own entitlement doc.
   - Users cannot set `allowed`, `maxDevices`, or un-revoke devices.
7. UX:
   - If blocked, show a simple "Access blocked" screen with reason.
   - If signed out, show login screen with Google as the primary action and optional email/password entry.
8. Offline policy:
   - Implement grace window: if last validation < 3 days, allow offline use.
   - If offline beyond grace, block and ask to connect internet.

## Data model (Firestore)
- users/{uid}:
  - allowed: boolean
  - maxDevices: number (`1` remains the compatibility default if older entitlement docs omit it)
  - expiresAt: number (0 means no expiry)
  - registeredDeviceIds: array<string> (optional compatibility field; client registration may only update this field, never entitlement controls)
- users/{uid}/devices/{deviceId}:
  - revoked: boolean
  - registeredAt: number
  - lastSeenAt: number
  - model: string
  - sdk: number

## Device ID policy
- Use a generated install UUID stored locally (SharedPreferences/DataStore).
- Do not use IMEI.
- ANDROID_ID may be logged for debugging only, not used as primary key.

## Acceptance tests
1. Fresh install on authorized device -> login -> allowed -> app opens.
2. Same user on second device when maxDevices=1 -> sign-in still succeeds when `allowed=true`; device registration may be logged best-effort.
3. Revoked device record -> sign-in still succeeds when `allowed=true`; revocation remains admin-visible data, not a hard access block in current behavior.
4. allowed=false -> blocked.
5. Offline < grace window -> app opens.
6. Offline > grace window -> blocked until online.
