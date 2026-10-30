package chemos.chem_os.services;

import chemos.chem_os.auth.model.User;
import chemos.chem_os.exception.EmailServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Only place that talks to the email-campaign service. Identity headers are always built here
// from the database user; nothing from the browser request is ever copied into them.
@Slf4j
@Service
public class EmailServiceClient {

    static final List<String> ALLOWED_ROOTS = List.of("campaigns", "customers", "products", "templates", "mailbox");
    private static final String IMPORT_PATH = "/customers/import";

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final AuditLogService auditLogService;

    public EmailServiceClient(@Qualifier("emailRestClient") RestClient restClient,
                              @Value("${app.email-service.base-url:}") String baseUrl,
                              @Value("${app.email-service.api-key:}") String apiKey,
                              AuditLogService auditLogService) {
        this.restClient = restClient;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.auditLogService = auditLogService;
    }

    // path is the part after /api/v1/email, e.g. "/campaigns/5". Query string is passed as-is.
    public ResponseEntity<byte[]> forward(HttpMethod method, String path, String query,
                                          byte[] body, String contentType, User user) {
        assertConfigured();
        String safePath = validatePath(path);
        HttpHeaders identity = identityHeaders(user);

        RestClient.RequestBodySpec spec = restClient.method(method)
                .uri(buildUri(safePath, query))
                .headers(h -> h.addAll(identity));
        if (body != null && body.length > 0) {
            spec.contentType(contentType == null ? MediaType.APPLICATION_JSON : MediaType.parseMediaType(contentType));
            spec.body(body);
        }

        ResponseEntity<byte[]> response = execute(spec);
        auditIfMutating(method, safePath, response.getStatusCode());
        return response;
    }

    // Spring has already consumed the multipart body by the time a controller runs, so the CSV
    // is received as a MultipartFile and re-posted to the email service as a new multipart body.
    public ResponseEntity<byte[]> importCustomers(MultipartFile file, User user) {
        assertConfigured();
        HttpHeaders identity = identityHeaders(user);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new EmailServiceException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
        String filename = file.getOriginalFilename() == null ? "customers.csv" : file.getOriginalFilename();
        MultipartBodyBuilder parts = new MultipartBodyBuilder();
        parts.part("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        }).contentType(file.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(file.getContentType()));

        RestClient.RequestBodySpec spec = restClient.post()
                .uri(buildUri(IMPORT_PATH, null))
                .headers(h -> h.addAll(identity))
                .contentType(MediaType.MULTIPART_FORM_DATA);
        spec.body(parts.build());

        ResponseEntity<byte[]> response = execute(spec);
        auditIfMutating(HttpMethod.POST, IMPORT_PATH, response.getStatusCode());
        return response;
    }

    private ResponseEntity<byte[]> execute(RestClient.RequestBodySpec spec) {
        try {
            return spec.exchange((request, response) -> mapResponse(
                    response.getStatusCode(), response.getHeaders(), response.getBody().readAllBytes()));
        } catch (EmailServiceException e) {
            throw e;
        } catch (ResourceAccessException e) {
            boolean timeout = e.getCause() instanceof SocketTimeoutException;
            log.error("Email service unreachable ({}): {}", timeout ? "timeout" : "connection", e.getMessage());
            throw new EmailServiceException(timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                    "Email service unavailable");
        } catch (Exception e) {
            log.error("Email service call failed", e);
            throw new EmailServiceException(HttpStatus.BAD_GATEWAY, "Email service unavailable");
        }
    }

    // 401 from upstream means ChemOS sent a wrong key or missing identity. Never surface it as
    // 401: the frontend would treat it as an expired session and log the user out.
    ResponseEntity<byte[]> mapResponse(HttpStatusCode status, HttpHeaders upstream, byte[] payload) {
        int code = status.value();
        if (code == 401) {
            log.error("Email service rejected ChemOS credentials (401): check EMAIL_SERVICE_API_KEY / identity headers");
            throw new EmailServiceException(HttpStatus.BAD_GATEWAY, "Email service unavailable");
        }
        if (code >= 500) {
            log.error("Email service returned {}", code);
            throw new EmailServiceException(HttpStatus.BAD_GATEWAY, "Email service unavailable");
        }
        ResponseEntity.BodyBuilder out = ResponseEntity.status(status);
        MediaType type = upstream.getContentType();
        if (type != null) {
            out.contentType(type);
        }
        String total = upstream.getFirst("X-Total-Count");
        if (total != null) {
            out.header("X-Total-Count", total);
        }
        return out.body(payload);
    }

    HttpHeaders identityHeaders(User user) {
        if (user == null) {
            throw new EmailServiceException(HttpStatus.UNAUTHORIZED, "Unauthorized - please log in");
        }
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        if (email.isEmpty()) {
            throw new EmailServiceException(HttpStatus.CONFLICT,
                    "Your ChemOS profile has no email address. Ask an administrator to add one before using Email Sender.");
        }
        if (!isPrintableAscii(email) || !isPrintableAscii(user.getUsername())) {
            throw new EmailServiceException(HttpStatus.CONFLICT,
                    "Your ChemOS username or email contains unsupported characters for Email Sender.");
        }
        HttpHeaders h = new HttpHeaders();
        h.set("X-User-Id", user.getId().toString());
        h.set("X-Username", user.getUsername());
        // Names may be non-ASCII; the email service falls back to the username when absent.
        if (user.getName() != null && !user.getName().isBlank() && isPrintableAscii(user.getName())) {
            h.set("X-User-Name", user.getName());
        }
        h.set("X-User-Email", email);
        h.set("X-User-Role", user.getRole().getName());
        return h;
    }

    static String validatePath(String path) {
        if (path == null || !path.startsWith("/")) {
            throw new EmailServiceException(HttpStatus.NOT_FOUND, "Not found");
        }
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.contains("..") || lower.contains("%2e") || lower.contains("%2f") || lower.contains("%5c")
                || lower.contains("\\") || lower.contains(";") || lower.contains("//")) {
            throw new EmailServiceException(HttpStatus.BAD_REQUEST, "Invalid path");
        }
        String root = lower.substring(1).split("/", 2)[0];
        if (!ALLOWED_ROOTS.contains(root)) {
            throw new EmailServiceException(HttpStatus.NOT_FOUND, "Not found");
        }
        return path;
    }

    private URI buildUri(String path, String query) {
        try {
            return URI.create(baseUrl + "/api" + path + (query == null || query.isBlank() ? "" : "?" + query));
        } catch (IllegalArgumentException e) {
            throw new EmailServiceException(HttpStatus.BAD_REQUEST, "Invalid request");
        }
    }

    private void assertConfigured() {
        if (baseUrl.isEmpty() || apiKey.isEmpty()) {
            throw new EmailServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Email service is not configured");
        }
    }

    private static boolean isPrintableAscii(String s) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 || c > 0x7e) {
                return false;
            }
        }
        return true;
    }

    // Audit who sent a campaign / changed a mailbox / imported customers. Metadata only, never
    // request bodies, which can hold message content, tokens or recipient data.
    private void auditIfMutating(HttpMethod method, String path, HttpStatusCode status) {
        if (!status.is2xxSuccessful() || HttpMethod.GET.equals(method)) {
            return;
        }
        String root = path.substring(1).split("/", 2)[0].toLowerCase(Locale.ROOT);
        String entity;
        if (path.equals(IMPORT_PATH)) {
            entity = "EMAIL_CUSTOMER_IMPORT";
        } else if (root.equals("campaigns")) {
            entity = "EMAIL_CAMPAIGN";
        } else if (root.equals("mailbox")) {
            entity = "EMAIL_MAILBOX";
        } else {
            return;
        }
        auditLogService.log(method.name(), entity, path, null,
                Map.of("method", method.name(), "path", path, "status", status.value()));
    }
}
