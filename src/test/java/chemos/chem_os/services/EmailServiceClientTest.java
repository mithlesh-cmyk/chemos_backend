package chemos.chem_os.services;

import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.exception.EmailServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmailServiceClientTest {

    private static final String BASE = "https://email.test";
    private static final String KEY = "test-key";

    private MockRestServiceServer server;
    private AuditLogService audit;
    private EmailServiceClient client;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().defaultHeader("X-API-Key", KEY);
        server = MockRestServiceServer.bindTo(builder).build();
        audit = mock(AuditLogService.class);
        client = new EmailServiceClient(builder.build(), BASE, KEY, audit);
    }

    private User user(String name, String email) {
        Role role = new Role();
        role.setId("sal_man");
        role.setName("SALES_MANAGER");
        User u = new User();
        u.setId(userId);
        u.setUsername("jdoe");
        u.setName(name);
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    @Test
    void identityHeadersComeFromTheDatabaseUserAndApiKeyIsAlwaysSent() {
        server.expect(requestTo(BASE + "/api/campaigns?page=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-Key", KEY))
                .andExpect(header("X-User-Id", userId.toString()))
                .andExpect(header("X-Username", "jdoe"))
                .andExpect(header("X-User-Name", "Jane Doe"))
                .andExpect(header("X-User-Email", "jane@example.com"))
                .andExpect(header("X-User-Role", "SALES_MANAGER"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON)
                        .header("X-Total-Count", "7"));

        ResponseEntity<byte[]> response = client.forward(HttpMethod.GET, "/campaigns", "page=2", null, null,
                user("Jane Doe", "jane@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("7");
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8)).isEqualTo("[]");
        server.verify();
    }

    @Test
    void clientSuppliedIdentityIsNeverForwarded() {
        // forward() has no parameter through which browser headers could flow; the only identity
        // values on the wire are the ones asserted here, taken from the user entity.
        server.expect(requestTo(BASE + "/api/templates"))
                .andExpect(header("X-User-Role", "SALES_MANAGER"))
                .andExpect(header("X-API-Key", KEY))
                .andRespond(withSuccess());

        client.forward(HttpMethod.GET, "/templates", null, null, null, user("Jane", "jane@example.com"));
        server.verify();
    }

    @Test
    void nonAsciiNameIsOmittedButRequestStillWorks() {
        server.expect(requestTo(BASE + "/api/products"))
                .andExpect(headerDoesNotExist("X-User-Name"))
                .andExpect(header("X-Username", "jdoe"))
                .andRespond(withSuccess());

        client.forward(HttpMethod.GET, "/products", null, null, null, user("José", "jane@example.com"));
        server.verify();
    }

    @Test
    void postBodyAndContentTypeArePassedThroughAndAudited() {
        server.expect(requestTo(BASE + "/api/campaigns"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(content().string("{\"name\":\"x\"}"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

        ResponseEntity<byte[]> response = client.forward(HttpMethod.POST, "/campaigns", null,
                "{\"name\":\"x\"}".getBytes(StandardCharsets.UTF_8), "application/json",
                user("Jane", "jane@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(audit).log(eq("POST"), eq("EMAIL_CAMPAIGN"), eq("/campaigns"), eq(null), any());
    }

    @Test
    void upstream401BecomesBadGatewayNeverUnauthorized() {
        server.expect(requestTo(BASE + "/api/campaigns")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.forward(HttpMethod.GET, "/campaigns", null, null, null,
                user("Jane", "jane@example.com")))
                .isInstanceOfSatisfying(EmailServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void upstream403And409PassThrough() {
        server.expect(requestTo(BASE + "/api/campaigns")).andRespond(withStatus(HttpStatus.FORBIDDEN));
        server.expect(requestTo(BASE + "/api/campaigns"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"user has no email\"}"));

        User u = user("Jane", "jane@example.com");
        assertThat(client.forward(HttpMethod.GET, "/campaigns", null, null, null, u).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<byte[]> conflict = client.forward(HttpMethod.GET, "/campaigns", null, null, null, u);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(new String(conflict.getBody(), StandardCharsets.UTF_8)).contains("user has no email");
    }

    @Test
    void upstream5xxBecomesBadGateway() {
        server.expect(requestTo(BASE + "/api/campaigns")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.forward(HttpMethod.GET, "/campaigns", null, null, null,
                user("Jane", "jane@example.com")))
                .isInstanceOfSatisfying(EmailServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void userWithoutEmailGets409WithoutCallingTheService() {
        assertThatThrownBy(() -> client.forward(HttpMethod.GET, "/campaigns", null, null, null,
                user("Jane", null)))
                .isInstanceOfSatisfying(EmailServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        server.verify();
    }

    @Test
    void blankConfigurationAnswers503() {
        EmailServiceClient unconfigured = new EmailServiceClient(RestClient.create(), "", "", audit);

        assertThatThrownBy(() -> unconfigured.forward(HttpMethod.GET, "/campaigns", null, null, null,
                user("Jane", "jane@example.com")))
                .isInstanceOfSatisfying(EmailServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verifyNoInteractions(audit);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/campaigns/../actuator", "/campaigns/%2e%2e/x", "/campaigns/%2E%2E/x",
            "/campaigns\\..\\x", "/campaigns//x", "/campaigns;x=1"})
    void traversalAttemptsAreRejected(String path) {
        assertThatThrownBy(() -> client.forward(HttpMethod.GET, path, null, null, null,
                user("Jane", "jane@example.com")))
                .isInstanceOfSatisfying(EmailServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/actuator/health", "/users", "/", ""})
    void pathsOutsideTheAllowListAreNotFound(String path) {
        assertThatThrownBy(() -> client.forward(HttpMethod.GET, path, null, null, null,
                user("Jane", "jane@example.com")))
                .isInstanceOfSatisfying(EmailServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        server.verify();
    }
}
