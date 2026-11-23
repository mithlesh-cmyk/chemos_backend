package chemos.chem_os.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class EmailServiceConfig {

    public static final String API_KEY_HEADER = "X-API-Key";

    // The API key is a default header so it cannot be forgotten on any call. Starting a large
    // campaign is slow, hence the long read timeout.
    @Bean(name = "emailRestClient")
    public RestClient emailRestClient(@Value("${app.email-service.api-key:}") String apiKey) {
        // HttpURLConnection (SimpleClientHttpRequestFactory) rejects PATCH, so use the JDK HttpClient.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(60));

        RestClient.Builder builder = RestClient.builder().requestFactory(factory);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(API_KEY_HEADER, apiKey.trim());
        }
        return builder.build();
    }
}
