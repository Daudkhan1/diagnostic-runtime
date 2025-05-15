package app.api.diagnosticruntime.config;

import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SecurityPathsConfig {

    public static final List<String> EXCLUDED_PATHS = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/user/forgot-password",
            "/h2-console/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    );
}
