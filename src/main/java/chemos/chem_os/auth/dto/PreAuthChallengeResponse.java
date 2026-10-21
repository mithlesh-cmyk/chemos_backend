package chemos.chem_os.auth.dto;

// Returned by /auth/login once password validation passes. "status" is one of
// ENROLLMENT_REQUIRED (user has no active TOTP credential yet) or VERIFICATION_REQUIRED
// (user is already enrolled). preAuthToken must be sent as a Bearer token to whichever
// /auth/2fa/** endpoint is appropriate next; it grants no business-endpoint access.
public record PreAuthChallengeResponse(
        String status,
        String preAuthToken,
        String username
) {
}
