# Security Review

## Scope
Authentication and licensing model, entitlement enforcement, secrets handling, permission-sensitive capabilities, and operational hardening.

## Current state
- Firebase Auth plus Firestore entitlement validation and install-UUID device binding are implemented in code.
- Firestore rules exist to protect user entitlements from client-side mutation.
- This pass added sanitized logs around licensing validation and backup or restore operations.
- Startup now fails into a deterministic route instead of depending on a fragile onboarding-state read.

## Findings

### S1. Firestore entitlement controls remain a strong baseline
- Severity: positive control
- Files: `firestore.rules`
- Why it matters: entitlement checks are only credible if clients cannot grant themselves access.
- Recommended fix: keep this rule posture and include rules deployment verification in release operations.
- Implement timing: later for operational automation
- Change risk: low
- Status: still valid

### S2. Firebase configuration in source control still requires explicit operational governance
- Severity: high
- Files:
  - `app/google-services.json`
  - Firebase console configuration outside the repo
- Why it matters: Firebase API keys are not secret by design, but production safety still depends on package-name restrictions, SHA certificate fingerprints, and enabled API scope.
- Recommended fix: verify Android app restrictions, rotate if prior restriction state is unknown, and keep signing-fingerprint changes in the release runbook.
- Implement timing: later, outside this code-only pass
- Change risk: low to code, medium operationally
- Status: open

### S3. Licensing and backup logs are now present but still minimal
- Severity: medium
- Files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/licensing/LicensingRepository.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- Why it matters: stable reason-code logging improves triage, but there is still no centralized crash or telemetry backend documented in the repository.
- Recommended fix: keep the new sanitized logs as the baseline, then add a central crash and incident-reporting path later.
- Implement timing: partially implemented now, expand later
- Change risk: low
- Status: partially addressed

### S4. Overlay and microphone permissions remain high-sensitivity capabilities
- Severity: high
- Files and areas:
  - manifest permission declarations
  - helper overlay subsystem
  - helper capture and microphone flows
- Why it matters: user trust, store-policy compliance, and support burden all depend on clear permission rationale and safe default behavior.
- Recommended fix: validate permission flows on a device and keep helper features disabled until the user explicitly enables them.
- Implement timing: later
- Change risk: medium
- Status: open

### S5. Device-backed security validation is still required before final release sign-off
- Severity: high
- Files and areas:
  - licensing gate
  - release-signed Google Sign-In
  - offline grace and blocked-state flows
- Why it matters: security controls that depend on Firebase auth, device binding, and release signing cannot be proven from unit tests alone.
- Recommended fix: execute the checklist in `docs/requirements/licensing-verification-checklist.md` on a real device with the production signing setup.
- Implement timing: blocked in this environment
- Change risk: low to code, high if skipped operationally
- Status: open

## Security release checklist
1. Firestore rules are deployed from the reviewed source.
2. Firebase API keys are restricted to the Android package and release signing fingerprints.
3. Signed-out, allowed, blocked, revoked-device, and offline-grace licensing paths are manually verified.
4. Release-signed Google Sign-In is verified on a real device.
5. Backup and restore logs are reviewed to confirm they do not expose PII, secrets, or raw payloads.
6. Overlay and microphone permission flows are verified for clear rationale and safe default behavior.
