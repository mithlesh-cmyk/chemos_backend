package chemos.chem_os.controller;

import chemos.chem_os.auth.model.User;
import chemos.chem_os.services.CurrentUserService;
import chemos.chem_os.services.EmailServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmailProxyControllerAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurity {}

    private AnnotationConfigApplicationContext context;
    private EmailServiceClient client;
    private CurrentUserService currentUsers;

    @BeforeEach
    void setUp() {
        client = mock(EmailServiceClient.class);
        currentUsers = mock(CurrentUserService.class);
        context = new AnnotationConfigApplicationContext();
        context.register(MethodSecurity.class, EmailProxyController.class);
        context.registerBean(EmailServiceClient.class, () -> client);
        context.registerBean(CurrentUserService.class, () -> currentUsers);
        context.refresh();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("jdoe", null, AuthorityUtils.createAuthorityList(authorities)));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/email/campaigns");
        req.setQueryString("page=1");
        return req;
    }

    @Test
    void userWithoutEmailCampaignUseIsDenied() {
        authenticate("SALE_VIEW", "SALE_CREATE");
        EmailProxyController controller = context.getBean(EmailProxyController.class);

        assertThatThrownBy(() -> controller.forward(request(), null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.importCustomers(new MockMultipartFile("file", "a.csv", "text/csv", new byte[]{1})))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(client);
    }

    @Test
    void userWithEmailCampaignUseIsForwardedWithPathAndQuery() {
        authenticate("EMAIL_CAMPAIGN_USE");
        User user = new User();
        when(currentUsers.getCurrentUser()).thenReturn(Optional.of(user));
        when(client.forward(any(), any(), any(), any(), any(), any())).thenReturn(ResponseEntity.ok(new byte[0]));

        ResponseEntity<byte[]> response = context.getBean(EmailProxyController.class).forward(request(), null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(client).forward(eq(HttpMethod.GET), eq("/campaigns"), eq("page=1"), eq(null), eq(null), eq(user));
    }
}
