# Permission System Spec — Backend Changes Needed

## Context

Today, access control in chemos-app (frontend) is based on a single `role` string per user (e.g. `"ADMIN"`, `"PURCHASE_MANAGER"`), and the frontend guesses what a role can do by substring-matching the role name (e.g. `role.includes('ADMIN')`). This doesn't support **user-specific permission overrides** on top of a role, and it hardcodes role-name assumptions into the frontend.

We want to move to: **role permissions + per-user overrides, merged into one effective permission set, computed by the backend and sent to the frontend.** The frontend will only ever ask "does this user have permission X?" — it will not guess based on role names.

## Data model

Two things need to exist (as DB tables or however fits your current schema):

1. **Role → permissions** — each role has a set of permission keys (e.g. `PURCHASE_MANAGER` → `["purchase.view", "purchase.create", "purchase.edit"]`).
2. **User → permission overrides** — each user can have individual overrides on top of their role, each with an effect of `ALLOW` or `DENY`.

### Merge rule (precedence)

```
effective_permissions(user) =
    ( permissions(user.role) ∪ user.overrides[ALLOW] )  −  user.overrides[DENY]
```

In words: start with the role's permissions, add anything the user was explicitly granted beyond their role, then remove anything the user was explicitly denied. **An explicit user-level DENY always wins**, even if the role would normally allow it.

The frontend does not need to know about roles or overrides separately — it only ever receives the final merged list.

## API contract changes

### 1. `POST /auth/login` (existing) — add a `permissions` field to the response

```json
{
  "username": "asha",
  "role": "PURCHASE_MANAGER",
  "token": "eyJhbGciOi...",
  "permissions": [
    "purchase.view",
    "purchase.create",
    "purchase.edit",
    "sale.view"
  ]
}
```

### 2. `GET /auth/me` (new, recommended)

Same shape as above minus `token`, callable with the existing bearer token. Lets the frontend refresh a user's permissions mid-session (e.g. an admin changes their role/overrides without forcing a re-login).

```json
{
  "username": "asha",
  "role": "PURCHASE_MANAGER",
  "permissions": ["purchase.view", "purchase.create", "purchase.edit", "sale.view"]
}
```

### 3. `GET /auth/roles` (existing) — optionally extend with default permissions per role

Not required for phase 1, but useful if an admin UI for editing role permissions is built later:

```json
{
  "id": "role_123",
  "name": "PURCHASE_MANAGER",
  "displayName": "Purchase Manager",
  "permissions": ["purchase.view", "purchase.create", "purchase.edit"]
}
```

## Permission key naming convention

`resource.action`, lowercase, dot-separated. Proposed starting set, based on the pages that exist today — please review/adjust with frontend before finalizing:

| Key | Meaning |
|---|---|
| `purchase.view` | See purchase orders |
| `purchase.create` | Create a purchase order |
| `purchase.edit` | Edit a purchase order |
| `purchase.confirm` | Confirm a purchase order |
| `purchase.cancel` | Cancel a purchase order |
| `sale.view` | See sale orders |
| `sale.create` | Create a sale order |
| `sale.edit` | Edit a sale order |
| `sale.confirm` | Confirm a sale order |
| `sale.cancel` | Cancel a sale order |
| `purchase_sale_link.view` | View purchase-sale links |
| `purchase_sale_link.edit` | Edit purchase-sale links |
| `audit.view` | View audit log |
| `users.view` | View user list |
| `users.manage` | Create/edit/deactivate users, assign roles/overrides |

## Out of scope for this phase

Row-level / conditional permissions (e.g. "can edit only purchases they created") are **not** covered by this model — it only expresses "can do X in general," not "can do X to this specific record." If that's needed later, flag it early since it changes the shape of `permissions` from a flat string list to a list of `{ key, condition }` rules.

## What the frontend will do with this

- Store the `permissions` array from login/`/auth/me` alongside the existing `role` in its session state.
- Add a single `can(permissionKey)` check used everywhere instead of today's ad-hoc role-string matching.
- This is UX-only — the backend must still enforce the same permission checks on every API endpoint regardless of what the frontend shows/hides.
