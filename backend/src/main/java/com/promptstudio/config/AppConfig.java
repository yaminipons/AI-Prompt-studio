package com.promptstudio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

/**
 * Application-wide configuration bringing together all the small,
 * standalone concerns that don't warrant their own dedicated config
 * class: CORS rules (so the vanilla JS frontend can call the API from
 * a different origin/port), the shared WebClient bean (used by
 * AiService to call the Gemini and ChromaDB HTTP APIs), MongoDB
 * auditing (powers the @CreatedDate/@LastModifiedDate annotations on
 * our entities), and the Swagger/OpenAPI documentation setup.
 */
@Configuration
@EnableMongoAuditing
public class AppConfig {

    /** Comma-separated list of explicitly allowed frontend origins, loaded from config (used for any non-localhost/production origins). */
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Defines the CORS policy for the entire application.
     * <p>
     * During local development, tools like VS Code Live Server often
     * change ports between runs (5500, 5501, 5502...) or switch between
     * "localhost" and "127.0.0.1". Matching those with a fixed exact-string
     * list in {@code app.cors.allowed-origins} is fragile and breaks
     * silently whenever the port changes. To avoid this entire class of
     * bug, any origin on localhost or 127.0.0.1 (on any port) is trusted
     * via {@code setAllowedOriginPatterns}, which — unlike
     * {@code setAllowedOrigins} — supports wildcard patterns while still
     * being compatible with {@code allowCredentials(true)}.
     * <p>
     * Any additional origins configured explicitly via
     * {@code app.cors.allowed-origins} (e.g. a real deployed frontend
     * domain in production) are also honored on top of the localhost
     * patterns.
     *
     * @return the configured CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> explicitOrigins = Arrays.asList(allowedOrigins.split(","));

        List<String> originPatterns = new java.util.ArrayList<>(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://localhost:*",
                "https://127.0.0.1:*"
        ));
        originPatterns.addAll(explicitOrigins);

        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Provides a shared, reactive WebClient instance used by AiService
     * to make outbound HTTP calls to the Google Gemini API and (when
     * enabled) the ChromaDB REST API. A generous in-memory buffer size
     * is configured since Gemini responses can be moderately large.
     *
     * @return the configured WebClient bean
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
    }

    /**
     * Configures the OpenAPI/Swagger documentation page, including a
     * Bearer JWT security scheme so protected endpoints can be tested
     * directly from the Swagger UI by pasting in a token.
     *
     * @return the configured OpenAPI bean
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("AI Prompt Engineering Studio API")
                        .description("REST API documentation for the AI Prompt Engineering Studio backend")
                        .version("1.0.0")
                        .contact(new Contact().name("AI Prompt Engineering Studio")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}