package chemos.chem_os.exception;

import org.springframework.http.HttpStatus;

// Raised by EmailServiceClient when the email service cannot be used or answers in a way the
// caller must see as a ChemOS-level error (e.g. 503 not configured, 502 bad upstream auth).
public class EmailServiceException extends RuntimeException {

    private final HttpStatus status;

    public EmailServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
