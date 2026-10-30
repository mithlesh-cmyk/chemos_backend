package chemos.chem_os.controller;

import chemos.chem_os.auth.model.User;
import chemos.chem_os.exception.EmailServiceException;
import chemos.chem_os.services.CurrentUserService;
import chemos.chem_os.services.EmailServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

// The browser keeps using its normal ChemOS token; this controller forwards allow-listed calls
// to the email service with ChemOS-built identity headers (see EmailServiceClient).
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/email")
public class EmailProxyController {

    private static final String PREFIX = "/api/v1/email";

    private final EmailServiceClient emailClient;
    private final CurrentUserService currentUserService;

    @PreAuthorize("hasAuthority('EMAIL_CAMPAIGN_USE')")
    @PostMapping(value = "/customers/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> importCustomers(@RequestParam("file") MultipartFile file) {
        return emailClient.importCustomers(file, currentUser());
    }

    @PreAuthorize("hasAuthority('EMAIL_CAMPAIGN_USE')")
    @RequestMapping(value = "/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                    RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<byte[]> forward(HttpServletRequest req,
                                          @RequestBody(required = false) byte[] body) {
        String contentType = req.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
            // Spring already consumed the stream; only the dedicated import endpoint handles files.
            throw new EmailServiceException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "File uploads are only supported on /customers/import");
        }
        String path = req.getRequestURI().substring(PREFIX.length());
        return emailClient.forward(HttpMethod.valueOf(req.getMethod()), path,
                req.getQueryString(), body, contentType, currentUser());
    }

    private User currentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new EmailServiceException(HttpStatus.UNAUTHORIZED, "Unauthorized - please log in"));
    }
}
