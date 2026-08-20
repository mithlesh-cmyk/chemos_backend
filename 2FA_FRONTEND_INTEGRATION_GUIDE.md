# 2FA Login Integration Guide (Frontend)

## What changed

Login used to be one step: `POST /auth/login` → full access token.

It is now **two steps**. Step 1 checks the password only and never returns a usable token — it returns a short-lived **pre-auth token** instead. Step 2 (a TOTP code from an authenticator app, or a backup code) must be completed with that pre-auth token before a real access token is issued.

The pre-auth token:
- Only proves "this username's password was correct."
- Grants **zero** access to any business API — sending it to any endpoint other than `/auth/2fa/**` gets rejected.
- Expires in **5 minutes**. If step 2 isn't completed in time, the user has to call `/auth/login` again to get a fresh one.

---

## The two possible paths after login

Every user hits one of these two paths, determined by the `status` field returned from `/auth/login`:

- **`ENROLLMENT_REQUIRED`** — user has never set up 2FA. Show the QR-code setup screen.
- **`VERIFICATION_REQUIRED`** — user already has 2FA enabled. Show the "enter your code" screen.

---

## Endpoints

### 1. `POST /api/v1/auth/login`
Step 1. No auth header needed.

**Request**
```json
{ "username": "string", "password": "string" }
```

**Response `200`**
```json
{
  "status": "ENROLLMENT_REQUIRED",   // or "VERIFICATION_REQUIRED"
  "preAuthToken": "eyJ...",
  "username": "string"
}
```

**Errors**: `401` if username/password is wrong, or account is disabled.

**Frontend action**: keep `preAuthToken` in memory (a variable/state, **not** localStorage — see Security Notes) and branch UI based on `status`.

---

### 2. `POST /api/v1/auth/2fa/enroll/init`
Only called when `status == "ENROLLMENT_REQUIRED"`. Generates a new TOTP secret for this user.

**Headers**: `Authorization: Bearer <preAuthToken>`
**Request body**: none

**Response `200`**
```json
{
  "secretBase32": "P6HDJNG2XEXMTV3CHLWUI7KJNHK2R33J",
  "otpauthUri": "otpauth://totp/ChemOS:admin?secret=...&issuer=ChemOS&algorithm=SHA1&digits=6&period=30"
}
```

**Frontend action**: render `otpauthUri` as a QR code (any QR library, e.g. `qrcode.react`) for the user to scan with Google Authenticator / Microsoft Authenticator / Authy. Also show `secretBase32` as selectable text below the QR, for users who can't scan (manual entry into their app).

**Errors**: `409 Conflict` if 2FA is already enabled for this user (shouldn't happen if you only call this on `ENROLLMENT_REQUIRED`, but handle it defensively — treat it the same as switching to the verify screen).

---

### 3. `POST /api/v1/auth/2fa/enroll/confirm`
Confirms the user actually scanned the QR and can produce a valid code. This is what **turns 2FA on**.

**Headers**: `Authorization: Bearer <preAuthToken>` (same one from step 1)
**Request**
```json
{ "code": "123456" }
```

**Response `200`**
```json
{
  "token": "eyJ...",             // full access token — login is now complete
  "username": "admin",
  "role": "ADMIN",
  "backupCodes": [
    "FVEF-76Z8", "R8F7-9XQZ", "3XGR-C78H", "EYBY-8Y24", "96KS-52R8",
    "FHF4-S62D", "KMHD-YZPR", "JEYC-8YW5", "UD46-8WW3", "BNEY-H7KP"
  ]
}
```

**This is the only response that will ever contain plaintext backup codes.** They cannot be retrieved again later.

**Frontend action**:
- On success, show a dedicated "Save your backup codes" screen — display all 10 codes clearly, with a copy-to-clipboard and/or download-as-text button, and a blocking confirmation ("I've saved these") before letting the user continue into the app.
- Then proceed exactly like a normal successful login: store `token`, set up the authenticated session, redirect into the app.

**Errors**:
- `401 Unauthorized` — wrong code. Let the user retry (show remaining attempts if you want, though the API doesn't return a count — just show a generic "invalid code" message).
- `429 Too Many Requests` — locked out after 5 wrong attempts, retry after the time in the message. Show it verbatim or parse it; it's a plain string like `"Too many failed attempts. Try again after 2026-08-04T15:08:38.858"`.
- `409 Conflict` — already enrolled.

---

### 4. `POST /api/v1/auth/2fa/login/verify`
Step 2 for a user who already has 2FA enabled (`status == "VERIFICATION_REQUIRED"` from login).

**Headers**: `Authorization: Bearer <preAuthToken>`
**Request**
```json
{ "code": "123456" }
```
`code` can be either a 6-digit TOTP code **or** a backup code in `XXXX-XXXX` format. The backend auto-detects which one it is — no separate field or toggle needed on the request. It just checks: is this 6 digits? Treat as TOTP. Otherwise, treat as a backup code.

**Response `200`**
```json
{ "token": "eyJ...", "username": "admin", "role": "ADMIN" }
```
Same as a normal completed login — store `token`, proceed into the app.

**Frontend action**: single input field for the code. Add a small "Use a backup code instead" link/toggle that just changes the input's placeholder/format hint (e.g. `123456` vs `XXXX-XXXX`) — it's the same field, same endpoint, same request shape either way.

**Errors**: same `401` / `429` semantics as enroll/confirm above.

---

### 5. `PATCH /api/v1/auth/users/{username}/2fa/reset`
Admin-only. Used when a user loses their phone/device and needs to re-enroll from scratch.

**Headers**: `Authorization: Bearer <full ADMIN access token>` (requires `USER_MANAGEMENT` permission)
**Request body**: none

**Response `200`**
```json
{ "username": "string", "totpEnabled": false, "backupCodesRemaining": 0 }
```

**Frontend action**: add a "Reset 2FA" button on the user management screen (wherever admins manage users today). Confirm with a dialog first — this immediately invalidates the user's current TOTP secret and all their backup codes; their next login will show the QR enrollment screen again.

---

## Screens to build

1. **Login screen** — unchanged fields (username/password), but on submit, branch on `status` instead of storing a token directly.
2. **Enroll — Scan QR screen** — shown when `status === "ENROLLMENT_REQUIRED"`. Renders `otpauthUri` as QR + shows `secretBase32` as fallback text. Has a code input + submit, calling enroll/confirm.
3. **Backup codes screen** — shown once, immediately after a successful enroll/confirm. Non-skippable without explicit acknowledgment. Never shown again after this.
4. **Verify code screen** — shown when `status === "VERIFICATION_REQUIRED"`. Single code input (accepts TOTP or backup code), with a toggle/hint for backup code format. Calls `2fa/login/verify`.
5. **Admin: Reset 2FA button** — in the existing user management UI.

Flow at a glance:

```
Login form
   |
   v
POST /auth/login
   |
   +-- status = ENROLLMENT_REQUIRED --> QR screen --> enroll/confirm --> Backup codes screen --> App
   |
   +-- status = VERIFICATION_REQUIRED --> Code entry screen --> 2fa/login/verify --> App
```

---

## Error handling reference

| Status | When | Suggested UI |
|---|---|---|
| `401` | Wrong password (login), wrong/expired pre-auth token, wrong TOTP/backup code | Generic "invalid credentials/code" message |
| `409` | Trying to enroll when already enrolled | Shouldn't surface normally; if it does, treat as "already set up," send to verify screen |
| `429` | 5 failed code attempts — locked out | Show the lockout message with the retry-after time from the response body |

If a pre-auth token expires mid-flow (`401` on enroll/init, enroll/confirm, or verify with no obvious wrong-code reason), the safest recovery is: send the user back to the login form to get a fresh `preAuthToken`, rather than trying to silently retry.

---

## Security notes for implementation

- **Never persist `preAuthToken` to localStorage/sessionStorage.** Keep it in memory (component state / a store) only — it's short-lived by design and doesn't need to survive a page refresh. If the user refreshes mid-2FA-flow, just send them back to the login form.
- **Never log `backupCodes` or the TOTP `code` field** to console, analytics, or error trackers.
- The backup codes screen should visually discourage screenshotting sensitive info casually (not enforceable, but worth a "copy" or "download .txt" affordance so users don't just leave the codes on screen).
- Full access `token` handling is unchanged from whatever the app already does today (same storage mechanism, same attach-to-requests logic) — only the *path to obtaining* that token changed.
