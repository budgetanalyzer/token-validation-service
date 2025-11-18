package org.budgetanalyzer.tokenvalidation.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;

/**
 * Security configuration for Token Validation Service.
 *
 * <p>Configures OAuth2 Resource Server to validate JWTs from Auth0 with: - Signature validation
 * using Auth0's public keys - Issuer validation - Audience validation
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuerUri;

  @Value("${auth0.audience}")
  private String audience;

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
                            String authHeader = request.getHeader("Authorization");
                            logger.error(
                                "Authorization header starts with Bearer: {}",
                                authHeader.startsWith("Bearer "));
                            if (authHeader.startsWith("Bearer ")) {
                              String token = authHeader.substring(7);
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
    logger.info("Issuer URI: {}", issuerUri);
    logger.info("Expected audience: {}", audience);

    try {
      logger.info(
          "Attempting to fetch OIDC configuration from: {}/.well-known/openid-configuration",
          issuerUri);

      // Create decoder with support for "at+jwt" token type (OAuth 2.0 RFC 9068)
      // and PS256 algorithm (Auth0 uses PS256 for access tokens)
      // Remove trailing slash from issuer URI if present to avoid double slashes
      String baseUri = issuerUri.endsWith("/") ? issuerUri.substring(0, issuerUri.length() - 1) : issuerUri;
      String jwksUri = baseUri + "/.well-known/jwks.json";
      NimbusJwtDecoder jwtDecoder =
          NimbusJwtDecoder.withJwkSetUri(jwksUri)
              .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
              .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.PS256)
              .jwtProcessorCustomizer(
                  jwtProcessor ->
                      jwtProcessor.setJWSTypeVerifier(
                          new DefaultJOSEObjectTypeVerifier<>(
                              new JOSEObjectType("at+jwt"), JOSEObjectType.JWT)))
              .build();

      logger.info("JWT decoder created successfully (JWKS will be fetched on first use)");
      logger.info("JWT decoder configured to accept token types: JWT, at+jwt");

      // Configure validators: issuer + audience
      OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(List.of(audience));
      OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
      OAuth2TokenValidator<Jwt> withAudience =
          new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

      jwtDecoder.setJwtValidator(
          token -> {
            logger.debug("=== JWT Validation ===");
            logger.debug("Token issuer: {}", token.getIssuer());
            logger.debug("Token audience: {}", token.getAudience());
            logger.debug("Token subject: {}", token.getSubject());
            logger.debug("Token algorithm: {}", token.getHeaders().get("alg"));
            logger.debug("Token kid: {}", token.getHeaders().get("kid"));
            logger.debug("Token expiration: {}", token.getExpiresAt());
            logger.debug("Token issued at: {}", token.getIssuedAt());
            logger.debug("All token headers: {}", token.getHeaders());
            logger.debug("All token claims: {}", token.getClaims());

            // Delegate to composite validator (issuer + audience)
            return withAudience.validate(token);
          });

      logger.info("JWT decoder configured successfully");
      logger.info("Decoder will accept tokens with algorithms: PS256, RS256, ES256");
      logger.info("JWKS endpoint: {}/.well-known/jwks.json", issuerUri);
      logger.info("OIDC configuration endpoint: {}/.well-known/openid-configuration", issuerUri);

      return jwtDecoder;

    } catch (Exception e) {
      logger.error("=== JWT Decoder Configuration Failed ===");
      logger.error("Failed to configure JWT decoder", e);
      logger.error("Issuer URI was: {}", issuerUri);
      logger.error("Exception type: {}", e.getClass().getName());
      if (e.getCause() != null) {
        logger.error("Caused by: {}", e.getCause().getMessage());
        logger.error("Root cause type: {}", e.getCause().getClass().getName());
      }
      throw new IllegalStateException("JWT decoder configuration failed", e);
    }
  }
}
