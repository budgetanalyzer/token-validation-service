package org.budgetanalyzer.tokenvalidation.config;

import java.io.IOException;
import java.util.Collections;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Filter to log ALL incoming HTTP requests before Spring Security processing.
 *
 * <p>This helps diagnose issues where requests might be rejected before reaching the controller.
 */
@Component
public class RequestLoggingFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (request instanceof HttpServletRequest httpRequest
        && response instanceof HttpServletResponse httpResponse) {

      // Log EVERY incoming request
      logger.debug("========================================");
      logger.debug("INCOMING REQUEST TO TOKEN VALIDATION SERVICE");
      logger.debug("========================================");
      logger.debug("Method: {}", httpRequest.getMethod());
      logger.debug("URI: {}", httpRequest.getRequestURI());
      logger.debug("Query String: {}", httpRequest.getQueryString());
      logger.debug("Remote Addr: {}", httpRequest.getRemoteAddr());
      logger.debug("Remote Host: {}", httpRequest.getRemoteHost());
      logger.debug("Remote Port: {}", httpRequest.getRemotePort());

      // Log ALL headers
      logger.debug("--- Headers ---");
      var headerNames = httpRequest.getHeaderNames();

      if (headerNames != null) {
        for (String headerName : Collections.list(headerNames)) {
          var headerValue = httpRequest.getHeader(headerName);
          // Truncate Authorization header for security, but show if it exists
          if ("Authorization".equalsIgnoreCase(headerName) && headerValue != null) {
            if (headerValue.startsWith("Bearer ")) {
              logger.debug(
                  "  {}: Bearer <token-length: {} chars>", headerName, headerValue.length() - 7);
            } else {
              logger.debug(
                  "  {}: <present but not Bearer, length: {} chars>",
                  headerName,
                  headerValue.length());
            }
          } else {
            logger.debug("  {}: {}", headerName, headerValue);
          }
        }
      } else {
        logger.debug("  (no headers)");
      }

      logger.debug("========================================");

      // Continue the filter chain
      chain.doFilter(request, response);

      // Log the response status
      logger.debug("RESPONSE STATUS: {}", httpResponse.getStatus());
      logger.debug("========================================");

    } else {
      // Not an HTTP request/response, just continue
      chain.doFilter(request, response);
    }
  }
}
