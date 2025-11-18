package org.budgetanalyzer.tokenvalidation.api;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication validation endpoint for NGINX auth_request.
 *
 * <p>NGINX will call this endpoint to validate JWTs before proxying requests to backend services.
 *
 * <p>Response codes: - 200 OK: JWT is valid - 401 Unauthorized: JWT is missing, expired, invalid
 * signature, or invalid audience
 */
@RestController
@RequestMapping("/auth")
public class AuthValidationController {

  private static final Logger logger = LoggerFactory.getLogger(AuthValidationController.class);

  /**
   * Validates the JWT in the Authorization header.
   *
   * <p>Spring Security will automatically validate the JWT. If it reaches this method, the JWT is
   * valid.
   *
   * <p>Returns X-JWT-User-Id header for NGINX to use in auth_request_set directive.
   *
   * @param authentication Spring Security authentication object (contains validated JWT)
   * @param request HTTP servlet request for logging
   * @return 200 OK with X-JWT-User-Id header if JWT is valid
   */
  @GetMapping("/validate")
  public ResponseEntity<Void> validate(Authentication authentication, HttpServletRequest request) {
    // Log incoming validation request
    String clientIp = request.getRemoteAddr();
    String forwardedFor = request.getHeader("X-Forwarded-For");
    String authHeader = request.getHeader("Authorization");
    String originalUri = request.getHeader("X-Original-URI");

    logger.info("=== Token Validation Request ===");
    logger.info("Client IP: {}", clientIp);
    logger.info("X-Forwarded-For: {}", forwardedFor != null ? forwardedFor : "none");
    logger.info("X-Original-URI: {}", originalUri != null ? originalUri : "none");
    logger.info("Authorization header present: {}", authHeader != null);

    // If we reach here, Spring Security has already validated:
    // 1. JWT signature (using Auth0 public keys)
    // 2. JWT expiration
    // 3. Issuer claim
    // 4. Audience claim

    // Extract user ID from JWT (sub claim) and return in response header
    if (authentication.getPrincipal() instanceof Jwt jwt) {
      String userId = jwt.getSubject();
      String email = jwt.getClaim("email");

      logger.info("JWT VALID - User: {} ({})", userId, email != null ? email : "no email");
      logger.info("Token expires: {}", jwt.getExpiresAt());
      logger.info("Setting X-JWT-User-Id header: {}", userId);
      logger.info("=== Validation Result: SUCCESS ===");

      // Return 200 OK with X-JWT-User-Id header for NGINX auth_request_set
      return ResponseEntity.ok()
          .header("X-JWT-User-Id", userId)
          .build();
    }

    // Fallback (should never reach here if JWT is valid)
    logger.warn("JWT validated but principal is not a Jwt object");
    logger.info("=== Validation Result: SUCCESS (no user ID) ===");
    return ResponseEntity.ok().build();
  }
}
