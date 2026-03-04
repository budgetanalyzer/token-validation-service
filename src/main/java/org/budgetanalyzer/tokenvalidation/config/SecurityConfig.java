package org.budgetanalyzer.tokenvalidation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Token Validation Service.
 *
 * <p>Validates gateway-minted JWTs using the session-gateway's JWKS endpoint. Signature
 * verification (RS256) is sufficient — these are trusted internal tokens, so issuer and audience
 * validation are not required.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwkSetUri;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    logger.info("Configuring OAuth2 Resource Server security");

    return http.authorizeHttpRequests(
            auth ->
                auth
                    // Allow health check endpoints without authentication
                    .requestMatchers("/actuator/health/**")
                    .permitAll()
                    // Allow JWT validation endpoint without authentication
                    // This endpoint validates JWTs, so it needs to accept unauthenticated requests
                    .requestMatchers("/auth/validate")
                    .permitAll()
                    // Require authentication for all other requests
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          logger.error("=== Authentication Failed ===");
                          logger.error("Request URI: {}", request.getRequestURI());
                          logger.error(
                              "Authorization header present: {}",
                              request.getHeader("Authorization") != null);
                          if (request.getHeader("Authorization") != null) {
                            var authHeader = request.getHeader("Authorization");

                            logger.error(
                                "Authorization header starts with Bearer: {}",
                                authHeader.startsWith("Bearer "));
                            if (authHeader.startsWith("Bearer ")) {
                              var token = authHeader.substring(7);

                              logger.error("Token length: {}", token.length());
                              // Log first 50 chars of token for debugging
                              logger.error(
                                  "Token preview: {}...",
                                  token.length() > 50 ? token.substring(0, 50) : token);
                            }
                          }
                          logger.error("Authentication exception: {}", authException.getMessage());
                          logger.error("Exception type: {}", authException.getClass().getName());
                          if (authException.getCause() != null) {
                            logger.error("Caused by: {}", authException.getCause().getMessage());
                            logger.error(
                                "Root cause type: {}",
                                authException.getCause().getClass().getName());
                          }

                          // Default behavior - return 401
                          response.setStatus(401);
                          response.setContentType("application/json");
                          response
                              .getWriter()
                              .write(
                                  "{\"error\":\"Unauthorized\",\"message\":\""
                                      + authException.getMessage()
                                      + "\"}");
                        })
                    .jwt(jwt -> jwt.decoder(jwtDecoder())))
        .build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    logger.info("=== JWT Decoder Configuration ===");
    logger.info("JWKS URI: {}", jwkSetUri);

    // Gateway-minted JWTs use RS256 — signature verification via JWKS is sufficient.
    // No issuer or audience validation needed for trusted internal tokens.
    var jwtDecoder =
        NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
            .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
            .build();

    logger.info("JWT decoder configured successfully");
    logger.info("JWKS endpoint: {}", jwkSetUri);

    return jwtDecoder;
  }
}
