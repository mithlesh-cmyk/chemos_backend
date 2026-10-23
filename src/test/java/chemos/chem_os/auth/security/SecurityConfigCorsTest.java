package chemos.chem_os.auth.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    private final CorsFilter filter = new CorsFilter(
            new SecurityConfig(mock(JwtAuthFilter.class)).corsConfigurationSource());

    @ParameterizedTest
    @ValueSource(strings = {
            "https://sumairo-os.org.in",
            "https://dev.sumairo-os.org.in",
            "http://localhost:3000",
            "http://localhost:5173",
            "http://35.154.133.62:3000",
            "http://35.154.133.62:3001",
            "http://13.206.139.250:3000",
            "http://13.206.139.250:3001"
    })
    void allowsLoginPreflightForTrustedOrigins(String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        request.addHeader(HttpHeaders.ORIGIN, origin);
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type, authorization");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(origin);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("POST");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .containsIgnoringCase("content-type")
                .containsIgnoringCase("authorization");
        assertThat(chain.getRequest()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://sumairo-os.org.in", "https://dev.sumairo-os.org.in"})
    void forwardsLoginPostForTrustedFrontendOrigins(String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader(HttpHeaders.ORIGIN, origin);
        request.setContentType("application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(origin);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @ParameterizedTest
    @CsvSource({
            "https://untrusted.example, OPTIONS",
            "https://untrusted.example, POST",
            "http://sumairo-os.org.in, OPTIONS",
            "http://sumairo-os.org.in, POST",
            "https://sumairo-os.org.in.attacker.example, OPTIONS",
            "https://sumairo-os.org.in.attacker.example, POST"
    })
    void rejectsUntrustedOrigins(String origin, String method) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/auth/login");
        request.addHeader(HttpHeaders.ORIGIN, origin);
        if ("OPTIONS".equals(method)) {
            request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEqualTo("Invalid CORS request");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(chain.getRequest()).isNull();
    }
}
