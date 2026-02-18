# Import Contacts Screen UI/UX Issues

Status: Open

## Scope
- Route: `import_contacts`
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/ImportContactsScreen.kt`

## Owner goals
- Import valid customers quickly.
- Avoid duplicate customers.
- Understand what will be imported before confirming.

## Open issues

### P1
- Current flow lacks pre-import duplicate warnings (name/phone conflicts).
- Large contact lists need faster navigation aids.
- Select-all behavior is clear but could show stronger visible scope feedback.

### P2
- No preview of normalization results before import.
- No post-import summary of created vs skipped records.

## Acceptance criteria
- Duplicate/conflict outcomes are shown before import.
- Import summary is shown after completion.
- Selection scope is always explicit (all vs filtered).
