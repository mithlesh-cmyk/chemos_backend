# Email Sender — Frontend Build Prompt & Integration Guide

> Paste this whole file as the prompt/spec for whoever (or whatever) builds the Email Sender UI inside the ChemOS frontend.
> Everything here is derived from the actual backend code in two repos:
> - **ChemOS backend** (`chem-os`) — `EmailProxyController`, `EmailServiceClient`, `EmailServiceConfig`, `SecurityConfig`, `DataSeeder`, `AuthService`
> - **Email platform** (`emial_sender/email-platform`) — the campaign service ChemOS proxies to

---

## 1. Your task

Build an **"Email Sender"** module inside the existing ChemOS web app (https://sumairo-os.org.in, dev: https://dev.sumairo-os.org.in). It lets a sales user run B2B email campaigns for chemicals:

1. Connect their own Gmail/Workspace mailbox (so customer replies are captured)
2. Manage customers (single add + CSV bulk import)
3. Manage products (chemicals)
4. Browse email templates
5. Create a campaign (chemical + base offer price)
6. Add customers as recipients, attach a template pool
7. Personalize each recipient (template, name, price, subject, body)
8. Send, then watch per-recipient delivery status

The browser **only ever talks to the ChemOS backend**, using the normal ChemOS JWT. It never talks to the email platform directly and never sees the email platform's API key.

---

## 2. Architecture — what the frontend must understand

```
Browser ──(Bearer ChemOS JWT)──▶ ChemOS  /api/v1/email/**  ──(X-API-Key + X-User-* headers)──▶ Email platform /api/**
                                  │  checks EMAIL_CAMPAIGN_USE
                                  │  builds identity headers from the DB user
                                  ▼
                                audit log (mutations only)
```

- ChemOS exposes a **transparent proxy**: `/api/v1/email/{rest}` → email platform `/api/{rest}`.
  Example: `GET /api/v1/email/campaigns/5/recipients?page=0` → `GET /api/campaigns/5/recipients?page=0`.
- **Allow-listed first path segments only:** `campaigns`, `customers`, `products`, `templates`, `mailbox`. Anything else → `404 Not Found`. Paths containing `..`, `//`, `;`, `\`, `%2e`, `%2f`, `%5c` → `400 Invalid path`.
- Methods proxied: `GET, POST, PUT, PATCH, DELETE`.
- Request body is forwarded as raw bytes with your `Content-Type`; response body and `Content-Type` are returned as-is.
- Identity (user id, username, name, email, role) is built **server-side from the database user**. Never try to send `X-User-*` or `X-API-Key` headers from the browser; they are ignored/never copied.

### Base URL

Use the same ChemOS API base URL the rest of the app already uses, plus `/api/v1/email`. (Backend ports: dev `8082`, prod `8083`. Do not hard-code hosts; read from the app's existing env/config.)

All calls: `Authorization: Bearer <ChemOS full access token>` (the token from the 2FA flow; see the Auth guide in `README.md`).

---

## 3. Access control (who can see this module)

| Layer | Rule |
|---|---|
| ChemOS | User needs permission code **`EMAIL_CAMPAIGN_USE`** (in `GET /api/v1/auth/me` → `permissions[]`). Granted by default to `ADMIN` (super role) and `SALES_MANAGER`. Admins can grant it to other roles. |
| Email platform | Role header must be **`ADMIN` or `SALES_MANAGER`**; any other role → `403`. |
| Ownership | A `SALES_MANAGER` only sees/changes **their own** campaigns (someone else's → `404`). `ADMIN` can access all. |

**UI rules**
- Show the "Email Sender" nav item/route only if `permissions` includes `EMAIL_CAMPAIGN_USE`. This is UX only; the backend enforces it.
- A user can hold `EMAIL_CAMPAIGN_USE` but have a role that is neither `ADMIN` nor `SALES_MANAGER` (e.g. a custom role). They will get `403` from every call. Handle this with a clear "Your role isn't allowed to use Email Sender" empty state, not a generic error.

---

## 4. Error handling (important — three different error shapes)

| Source | Status | Body shape |
|---|---|---|
| ChemOS security (no/invalid token) | `401` | `{"error":"Unauthorized - please log in"}` |
| ChemOS security (missing `EMAIL_CAMPAIGN_USE`) | `403` | `{"error":"Access denied - insufficient permissions"}` |
| ChemOS proxy-level (`EmailServiceException`) | various | `{"error":"<reason phrase>","message":"<text>","timestamp":"<ISO local datetime>"}` |
| Email platform (passed through) | `400/403/404/409…` | Spring default: `{"timestamp","status","error","path"}` — **no `message`** (internal messages hidden). Branch on HTTP status, not text. |

### Proxy-level errors you must handle

| Status | `message` | Meaning / UI behaviour |
|---|---|---|
| `401` | `Unauthorized - please log in` | **ChemOS session** expired/invalid → log the user out, redirect to login. |
| `409` | `Your ChemOS profile has no email address. Ask an administrator to add one before using Email Sender.` | Show blocking banner; the user's email is also their **From address**. Link to profile / ask admin. |
| `409` | `Your ChemOS username or email contains unsupported characters for Email Sender.` | Same style banner (non-ASCII in username/email). |
| `415` | `File uploads are only supported on /customers/import` | Developer bug: multipart sent to a non-import path. |
| `400` | `Invalid path` / `Invalid request` / `Could not read the uploaded file` | Developer bug or bad file. |
| `404` | `Not found` | Path not in allow-list. |
| `502` | `Email service unavailable` | Upstream down, returned 5xx, or **rejected ChemOS credentials**. Show "Email service is temporarily unavailable, try again" with retry. **Never treated as 401**, so don't log the user out on 502. |
| `503` | `Email service is not configured` | Environment misconfigured (no URL/API key). Show "Email Sender isn't set up for this environment". |
| `504` | `Email service unavailable` | Upstream timeout (connect 3 s, read **60 s**). For `send`, the work may still have been queued — after a 504, **re-fetch recipients** before offering a retry (see §7.6). |

### Rules for the HTTP client
- A `401` from `/api/v1/email/**` means the ChemOS token is bad → logout. Upstream 401s are already converted to 502 by the proxy, so any 401 you see is a real session problem.
- Upstream `4xx` (400/403/404/409) are passed through unchanged. `5xx` from upstream are always converted to `502`.
- Show validation failures (`400`) as a generic "Please check the highlighted fields" and validate client-side to the limits in §6 so users rarely hit them.

---

## 5. CORS / headers gotcha (needs a tiny backend change)

`SecurityConfig.corsConfigurationSource()` currently has `setExposedHeaders(List.of("Authorization"))` only.
The proxy forwards the **`X-Total-Count`** header on paged lists, but **a cross-origin browser cannot read it** unless it is exposed.

Either:
- **(Recommended)** ask the backend to add `"X-Total-Count"` to the exposed headers, or
- Fallback in the UI: page with `page=0,1,2…` and stop when a page returns fewer than `size` items.

Build a small `fetchAllPages()` helper that prefers `X-Total-Count` if readable and falls back to the short-page rule.

Allowed CORS origins are fixed in `SecurityConfig` (prod/dev domains, `localhost:3000`, `localhost:5173`, a few IP:port pairs). A new dev origin needs a backend change.

---

## 6. API reference (as seen from the browser)

All paths below are relative to `/api/v1/email`. JSON unless stated. Timestamps are ISO-8601 (`2026-09-07T10:15:30.123Z`).

### 6.1 Mailbox — `/mailbox` (always the caller's own)

| Call | Purpose |
|---|---|
| `GET /mailbox` | Connection health |
| `POST /mailbox` body `{ "appPassword": "abcd efgh ijkl mnop" }` (max 255) → `200` empty | Connect or reconnect (restarts the listener) |
| `DELETE /mailbox` → `204` | Disconnect and clear stored credential |

`GET` response:
```json
{ "connected": true, "connectedAt": "2026-09-30T09:00:00Z",
  "consecutiveFailures": 0, "lastError": null, "lastErrorAt": null, "needsReconnect": false }
```
- `connected:false` → never connected / disconnected.
- `consecutiveFailures > 0` or `lastError` → configured but failing (auto-retries with backoff).
- `needsReconnect:true` → Google rejected login repeatedly (revoked app password). **Replies are NOT being captured.** Show a prominent "Reconnect your mailbox" state.
- There is **no "test connection" endpoint**: after `POST`, poll `GET` a few times (e.g. every 2 s for ~10 s) and show the result.
- The value is a Google **App Password** (needs 2-Step Verification), **not** the Gmail login password. Include a short help text with the path: Google Account → Security → App passwords.
- Never store, log, or echo the app password in the client. Clear the input after submit.

### 6.2 Products (chemicals) — `/products`

| Call | Notes |
|---|---|
| `GET /products?activeOnly=true` | Not paged. `[{id,name,hsCode,casNo,isActive,createdAt,updatedAt}]` |
| `GET /products/{id}` | `404` if missing |
| `POST /products` | `{id, name, hsCode?, casNo?}`; `id` is **caller-chosen string** (e.g. `UREA-46`), max 255, becomes the FK; `400` if it already exists |

### 6.3 Customers — `/customers`

| Call | Notes |
|---|---|
| `GET /customers?activeOnly=false&page=0&size=500` | Ordered by id. `size` max **1000**. Total in `X-Total-Count` (see §5). |
| `GET /customers/{id}` | `404` if missing |
| `POST /customers` | `{companyName (req, ≤255), contactName?, email (req, valid, ≤255), phone? (≤50)}`. Email is stored **lowercase**; `400` if already exists in any case. |
| `POST /customers/import` | `multipart/form-data`, field name **`file`**. This is the **only** multipart endpoint. |

Customer: `{id, companyName, contactName, email, phone, active, createdAt}`.

**CSV import rules**
- Header row must be exactly: `companyName,contactName,email,phone`
- `companyName` and `email` required per row; `contactName`/`phone` may be blank
- Duplicate emails (in file or DB) are **skipped, not errors**
- Response:
```json
{ "totalRows": 50, "imported": 47, "skipped": 3,
  "errors": [{ "rowNumber": 12, "email": "bad-email", "reason": "email is not a valid format" }] }
```
`rowNumber` is the real CSV line (header = line 1) → show a table "Row 12 – bad-email – reason". `400` for an empty file.
UI: drag-and-drop, client-side header check, "Download sample CSV" button, result summary card (imported / skipped / errors).

### 6.4 Templates — `/templates`

| Call | Notes |
|---|---|
| `GET /templates/masters` | All top-level templates. Picker list for the campaign pool and per-recipient choice. |
| `GET /templates/{masterId}/forks` | Named forks + personalized forks of a master (filter by `personalizedForCustomerId != null` client-side) |
| `GET /templates/{masterId}/personalized?customerId={id}` | `200` = that customer's saved personalized version; **`404` = none (expected, not an error)** |
| `POST /templates/fork` | `{masterId (req), campaignId?, name?, subjectLine?, htmlBody?}` → new named reusable template; blank fields fall back to master. Only masters can be forked (forking a fork → `400`). |

Template: `{id, name, subjectLine, htmlBody, active, parentTemplateId, personalizedForCustomerId, createdForCampaignId, createdAt}`.

**Placeholders** (inside `subjectLine` and `htmlBody`):
`{{customer_name}}`, `{{company_name}}`, `{{chemical_name}}`, `{{price}}`, `{{senderName}}`.
- `customer_name` and `price` can be overridden per recipient; the rest always come from records.
- In the **body**, values are HTML-escaped; in the **subject** they are inserted as plain text.
- `htmlBody` is trusted HTML that is emailed. Render previews inside a **sandboxed `<iframe srcDoc>`** (no scripts), never with `dangerouslySetInnerHTML` in the app DOM.
- There is **no server-side preview endpoint**. Implement client-side preview by substituting placeholders using the same rules above (use recipient override if present, else customer `contactName` / campaign `offerPrice`; `senderName` = logged-in user's name).

### 6.5 Campaigns — `/campaigns`

| Call | Body / notes |
|---|---|
| `POST /campaigns` | `{name (≤255), chemicalId (≤255, must exist), offerPrice (>0, ≤10 digits + 2 decimals)}` → full Campaign, status `DRAFT`. Owner is always the caller. |
| `POST /campaigns/{id}/recipients` | `{customerIds: [Long]}` (1–**5000** per call). Already-added ids are silently skipped → safe to call again. Unknown ids → `400`. Returns array of CampaignRecipient. |
| `GET /campaigns/{id}/recipients?page=0&size=500` | Paged (max 1000), total in `X-Total-Count`. `404` if campaign doesn't exist / isn't yours. |
| `PUT /campaigns/{id}/templates` | `{templateIds: [Long]}` (1–**100**). Sets the rotation pool; returns the pool as `EmailTemplate[]`. |
| `PATCH /campaigns/{id}/recipients/{recipientId}` | Any subset of `{templateId, overrideName (≤255), overridePrice, subjectLine, htmlBody (≤200000)}`. Returns updated CampaignRecipient. |
| `POST /campaigns/{id}/send` | No body → `200` empty. **Async.** |

Campaign: `{id, name, chemical:{...}, offerPrice, createdBy, templatePool, status, scheduledAt, createdAt}`
CampaignRecipient: `{id, customer:{...}, assignedTemplate:{...}|null, overrideRecipientName, overrideOfferPrice, status, trackingToken, sentAt, openedAt, clickedAt, repliedAt, createdAt}`

Enums
- `CampaignStatus`: `DRAFT, SCHEDULED, SENDING, COMPLETED, CANCELLED`
- `RecipientStatus`: `PENDING, SENT, DELIVERED, OPENED, CLICKED, REPLIED, BOUNCED, COMPLAINED, FAILED`
  (Open/click tracking is **not implemented**; `OPENED`/`CLICKED` won't occur today. Handle unknown values gracefully.)

PATCH semantics
- `templateId`: always send the **master's** id (never a fork id).
- `overrideName` / `overridePrice`: omit/null = keep default.
- `subjectLine` / `htmlBody`: saves/overwrites **one** personalized fork per (master, customer) and assigns it. **Only send them if the user actually edited the text** (compare against the prefilled value). Otherwise every recipient becomes a "personalization".
- `400` if the recipient is no longer `PENDING` (already sent) or doesn't belong to the campaign. Also `400 "Select a template before editing its body"` if you send body edits with no template selected.

Send preconditions → `400` otherwise: campaign status is `DRAFT` or `SCHEDULED`; at least one template in the pool; at least one `PENDING` recipient. Recipients you customized keep their template; the rest get one from the pool by round-robin.

---

## 7. Screens & flows to build

### 7.1 Gate + onboarding checklist (landing page of the module)
Checklist card with live status:
1. Profile has an email (else the `409` banner) — the From address
2. Mailbox connected & healthy (`GET /mailbox`)
3. At least one customer exists
4. At least one product exists
Disable "New campaign" until 1 and 2 are satisfied, with an explanation. Warn (don't block) if the mailbox isn't connected: *"Replies will not be captured, and there is no error if you skip this."*

### 7.2 Mailbox settings
Status badge (Connected / Failing / Needs reconnect / Not connected), last error + time, connect form (app password, masked input, help link/text), disconnect with confirm.

### 7.3 Customers
Paged table (company, contact, email, phone, active), search/filter client-side over loaded pages, "Add customer" modal (validate email, lowercase), "Import CSV" flow (§6.3).

### 7.4 Products
Table + "Add product" modal (id is caller-chosen, immutable, explain that).

### 7.5 Templates
Masters list with subject + sandboxed HTML preview, placeholder legend, "Fork as named template" action.

### 7.6 Campaign wizard (the core)
Stepper, persisted so the user can resume:
1. **Details** — name, chemical dropdown (`activeOnly=true`), offer price (2 decimals). `POST /campaigns`.
2. **Recipients** — multi-select picker over customers (virtualized list; chunk into ≤5000 ids per call). `POST /campaigns/{id}/recipients`.
3. **Templates** — multi-select masters (1–100) → `PUT /campaigns/{id}/templates`.
4. **Personalize** — table of recipients (`GET .../recipients`). Per row: template dropdown, name override, price override, "Edit message" drawer.
   - On template pick: `GET /templates/{masterId}/personalized?customerId=…` → `200` prefill with their past wording, `404` prefill with the master's text.
   - Remember the prefilled text; send `subjectLine`/`htmlBody` in the PATCH **only when changed**.
   - Confirm before saving a body edit: it overwrites the previous saved edit for that (template, customer) pair; there is no undo/history.
   - Show a live sandboxed preview with placeholders substituted.
   - Save per row (PATCH) with an inline saved/error indicator; debounce, and disable rows already not `PENDING`.
5. **Review & send** — summary (chemical, price, #recipients, #templates, #personalized, From address = user's email), confirm dialog, `POST /campaigns/{id}/send`. Disable the button while in flight (double-click = duplicate send risk; the API has no idempotency key).

**After send**
- Sending is asynchronous (queue → SES). Recipient statuses change over seconds/minutes. Poll `GET /campaigns/{id}/recipients` (e.g. every 5 s while any recipient is `PENDING`, back off to 15–30 s, stop when none are pending or the user leaves the page).
- Show status counts (Pending / Sent / Delivered / Replied / Bounced / Failed) and a per-row status chip with `sentAt` / `repliedAt`.
- After a `504`/network error on send: **do not blindly retry** — re-fetch recipients first; if they are no longer `PENDING`, the send went through.
- `SENT` means SES accepted it, not that it was delivered. Campaign status moves to `COMPLETED` automatically once every job is sent/dead-lettered, but **there is no endpoint to read campaign status** (see §8), so derive progress from recipients.

### 7.7 Campaign history
See §8 — requires client-side persistence because the API has no list endpoint.

---

## 8. Known backend gaps (design around these; don't invent endpoints)

The email platform has **no**:
- `GET /campaigns` (list) or `GET /campaigns/{id}` (detail/status) → store the ids of campaigns the user creates (e.g. `localStorage` keyed by ChemOS username, wrapped in try/catch) so "My campaigns" can reload their recipients. Ideally ask backend to add list/detail; design the UI so swapping to those is a small change.
- Campaign edit or cancel.
- Endpoint to read **reply text** (stored for a separate analysis service) — only the recipient's `REPLIED` status and `repliedAt` are visible. Don't build an inbox.
- Visibility into unmatched replies.
- Customer update/delete, product update/delete, template create/edit (only fork).
- Open/click tracking, unsubscribe, scheduling UI (`SCHEDULED`/`scheduledAt` exist but nothing sets them).

---

## 9. Related: transactional emails sent by ChemOS itself (no UI work, but affects user-admin screens)

Separate from campaigns, ChemOS (`EmailService`) sends SMTP emails when an admin manages users:
- **Create user** (`POST /api/v1/auth/users`) → welcome email with username, **plaintext password**, and login URL.
- **Edit user with `newPassword`** (`PATCH /api/v1/auth/users/{username}`) → "password changed" email with the new password.

Facts for the UI:
- `email` is **required** on create and update (valid format); duplicate email on update → `409 Email already in use`.
- Emails are sent **async after commit** and failures are only logged, so the API succeeds even if the mail doesn't arrive. Don't promise delivery; say "A welcome email has been sent to jane@…" → better: "We've attempted to email the credentials; share them manually if they don't arrive."
- Can be disabled per environment (`MAIL_ENABLED`).
- **Role names that matter for Email Sender:** the user's ChemOS role `name` must be `ADMIN` or `SALES_MANAGER` to pass the email platform's check. In the role dropdown/user form, hint when someone is given `EMAIL_CAMPAIGN_USE` on another role that it won't work.
- A user with no/blank email, or non-ASCII username/email, cannot use Email Sender (the `409`s in §4). Make the user-admin form validate email presence.

---

## 10. Non-functional requirements

- **Stack:** match the existing ChemOS frontend (framework, component library, state/query library, routing). Reuse its auth/HTTP wrapper; add an `emailApi` module on top of it with typed functions for every endpoint in §6 and typed errors for §4.
- **Typing:** define TypeScript types for every entity/enum in §6; treat enums as open (unknown value → neutral chip).
- **Server state:** use the existing data-fetching library with caching + invalidation (e.g. invalidate recipients after PATCH/add; masters/products rarely change).
- **Large lists:** customers can be thousands → paged fetch, virtualized pickers, never render everything.
- **Security:** never log tokens/app passwords; sandbox all HTML previews; no secrets or API keys in client code.
- **UX:** loading skeletons, empty states, inline error with retry, confirm dialogs for send / body overwrite / disconnect mailbox, toasts for success; responsive down to tablet at least.
- **Accessibility:** labelled inputs, keyboard-operable tables/dialogs, status not conveyed by colour alone.
- **Timeouts:** the proxy waits up to 60 s on the upstream; set the client timeout ≥ 65 s for `send`, normal elsewhere.

---

## 11. Suggested file/module layout (adapt to the repo)

```
features/email-sender/
  api/emailApi.ts            # typed endpoint functions + error mapping
  api/types.ts               # Customer, Product, EmailTemplate, Campaign, CampaignRecipient, enums
  hooks/                     # useMailbox, useCustomers (paged), useCampaignRecipients (polling), …
  pages/
    EmailHome.tsx            # onboarding checklist
    MailboxSettings.tsx
    Customers.tsx            # + ImportCsvDialog
    Products.tsx
    Templates.tsx
    CampaignWizard/          # steps 1–5
    CampaignDetail.tsx       # recipients + status polling
  components/
    TemplatePreview.tsx      # sandboxed iframe + placeholder substitution
    StatusChip.tsx
    PermissionGate.tsx       # EMAIL_CAMPAIGN_USE
```

---

## 12. Acceptance checklist

- [ ] Module hidden without `EMAIL_CAMPAIGN_USE`; clear message for disallowed roles (`403`)
- [ ] `409` (no email / bad characters) shows a blocking, actionable banner
- [ ] `401` logs out; `502/503/504` show retryable errors and **do not** log out
- [ ] Mailbox: connect, health polling, `needsReconnect` state, disconnect
- [ ] Customers: paged list, add, CSV import with per-row error table
- [ ] Products: list, add
- [ ] Templates: masters list, sandboxed preview, `personalized` `404` handled as "none"
- [ ] Wizard: create → recipients → templates → personalize → send
- [ ] PATCH sends `subjectLine`/`htmlBody` only when edited; always sends master `templateId`
- [ ] Send button single-flight; post-send polling; no blind retry after timeout
- [ ] Campaign ids persisted client-side to reload history; UI ready to switch to list/detail endpoints
- [ ] `X-Total-Count` handled with fallback until CORS exposes it
- [ ] No secrets in client code; HTML previews sandboxed
