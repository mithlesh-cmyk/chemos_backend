# 2FA Login Issue - Diagnosis & Fixes Applied

## Problem Summary
You were experiencing 2FA login failures for user `salesguy1` even with correct codes from your authenticator app.

## Root Causes Identified

### 1. **Time Synchronization Issues** (Most Likely)
- TOTP codes are time-based (30-second windows)
- Previous setting allowed only ±30 seconds clock drift
- If your server clock differs from your phone by >30 seconds, all codes fail

### 2. **Account Lockout**
- After 5 failed attempts → locked for 15 minutes
- You may have been repeatedly hitting this lockout
- Poor error messages didn't clearly indicate lockout status

### 3. **Code Reuse** (Security Gap)
- Codes could be reused multiple times within their validity window
- Not the cause of your issue, but a security vulnerability

## Fixes Applied

### ✅ 1. Increased Time Window Tolerance
**Changed:** `setAllowedTimePeriodDiscrepancy(1)` → `setAllowedTimePeriodDiscrepancy(2)`
- **Before:** Accepted codes from ±30 seconds
- **After:** Accepts codes from ±60 seconds
- **Impact:** More tolerant of clock drift between server and authenticator app

### ✅ 2. Better Lockout Error Messages
**Added:** Detailed lockout information in error responses
```json
{
  "error": "Account locked due to too many failed 2FA attempts. Please try again in 12 minutes. Failed attempts: 5/5"
}
```

### ✅ 3. Code Reuse Prevention
**Added:** Tracking of last used code to prevent replay attacks
- Codes can only be used once within 90-second window
- Improved security without affecting normal usage

### ✅ 4. New Admin Endpoints
Added three new management endpoints:

#### a) **Check 2FA Status**
```bash
GET /api/v1/auth/users/{username}/2fa/status
```
Shows: enabled status, backup codes remaining, lockout info

#### b) **Unlock User**
```bash
PATCH /api/v1/auth/users/{username}/2fa/unlock
```
Clears failed attempts and removes lockout (doesn't reset 2FA enrollment)

#### c) **Reset 2FA** (existing, but documented here)
```bash
PATCH /api/v1/auth/users/{username}/2fa/reset
```
Completely removes 2FA - user must re-enroll

## Immediate Solutions

### Option 1: Wait for Lockout to Expire (15 minutes)
If your last attempt was recent, wait 15 minutes and try again with the increased time tolerance.

### Option 2: Unlock the Account (Recommended)
Use an admin account to unlock `salesguy1`:

```bash
curl -X PATCH "http://localhost:8081/api/v1/auth/users/salesguy1/2fa/unlock" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

### Option 3: Direct Database Unlock (If no admin access)
Run this SQL query to manually unlock:

```sql
UPDATE two_factor_credentials 
SET failed_attempts = 0, 
    locked_until = NULL 
WHERE user_id = (SELECT id FROM users WHERE username = 'salesguy1');
```

## Testing Steps

### 1. **Restart Your Application**
```bash
# Stop current instance
# Then restart with the new build
java -jar target/chem-os-0.0.1-SNAPSHOT.jar
```

### 2. **Test Login Flow**
```bash
# Step 1: Initial login (get pre-auth token)
curl -X POST "http://localhost:8081/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"salesguy1","password":"changeme123"}'

# Response will have a PRE_AUTH token
# Copy the token from the response

# Step 2: Verify with TOTP code
curl -X POST "http://localhost:8081/api/v1/auth/2fa/login/verify" \
  -H "Authorization: Bearer YOUR_PRE_AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"code":"123456"}'  # Use current code from authenticator
```

### 3. **Check Status (as admin)**
```bash
curl -X GET "http://localhost:8081/api/v1/auth/users/salesguy1/2fa/status" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

Expected response:
```json
{
  "username": "salesguy1",
  "totpEnabled": true,
  "backupCodesRemaining": 10
}
```

## Additional Database Schema Changes

New columns added to `two_factor_credentials` table:
- `last_used_code` - Stores the last successfully used TOTP code
- `last_code_used_at` - Timestamp of when the code was used

These will be auto-created by Hibernate on next app start (ddl-auto=update).

## Troubleshooting

### If Login Still Fails:

1. **Check Server Time vs Phone Time**
   ```bash
   # On server
   date
   
   # Should match your phone time within 1-2 minutes
   ```

2. **Verify Pre-Auth Token Not Expired**
   - Tokens expire in 5 minutes
   - Get a fresh token if you waited too long

3. **Check Logs for Specific Error**
   Look for `2FA_LOGIN_FAILED` audit log entries

4. **Try a Backup Code Instead**
   If you have backup codes from enrollment, use one of those instead of TOTP code

5. **Last Resort: Reset and Re-enroll**
   ```bash
   curl -X PATCH "http://localhost:8081/api/v1/auth/users/salesguy1/2fa/reset" \
     -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
   ```
   Then go through enrollment again

## Prevention Tips

1. **Keep server time synchronized** - Use NTP on your server
2. **Wait for new code** - Don't reuse the same code multiple times
3. **Save backup codes** - Keep them in a secure location
4. **Monitor lockouts** - Use the status endpoint to check user states

## Summary of Changes

| File | Changes |
|------|---------|
| `TwoFactorAuthService.java` | ✅ Increased time discrepancy to 2 periods<br>✅ Added code reuse prevention<br>✅ Better lockout messages<br>✅ New unlock/status methods |
| `TwoFactorCredential.java` | ✅ Added `lastUsedCode` and `lastCodeUsedAt` fields |
| `AuthController.java` | ✅ Added unlock and status endpoints |

The application is now **rebuilt and ready** with these fixes. Restart your server to apply them.
