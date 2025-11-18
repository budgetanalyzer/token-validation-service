package org.budgetanalyzer.tokenvalidation.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
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
      logger.info("========================================");
      logger.info("INCOMING REQUEST TO TOKEN VALIDATION SERVICE");
      logger.info("========================================");
      logger.info("Method: {}", httpRequest.getMethod());
      logger.info("URI: {}", httpRequest.getRequestURI());
      logger.info("Query String: {}", httpRequest.getQueryString());
      logger.info("Remote Addr: {}", httpRequest.getRemoteAddr());
      logger.info("Remote Host: {}", httpRequest.getRemoteHost());
      logger.info("Remote Port: {}", httpRequest.getRemotePort());

      // Log ALL headers
      logger.info("--- Headers ---");
      Enumeration<String> headerNames = httpRequest.getHeaderNames();
      if (headerNames != null) {
        for (String headerName : Collections.list(headerNames)) {
          String headerValue = httpRequest.getHeader(headerName);
          // Truncate Authorization header for security, but show if it exists
          if ("Authorization".equalsIgnoreCase(headerName) && headerValue != null) {
            if (headerValue.startsWith("Bearer ")) {
              logger.info("  {}: Bearer <token-length: {} chars>", headerName,
                  headerValue.length() - 7);
            } else {
              logger.info("  {}: <present but not Bearer, length: {} chars>", headerName,
                  headerValue.length());
            }
          } else {
            logger.info("  {}: {}", headerName, headerValue);
          }
        }
      } else {
        logger.info("  (no headers)");
      }

      logger.info("========================================");

      // Continue the filter chain
      chain.doFilter(request, response);

      // Log the response status
      logger.info("RESPONSE STATUS: {}", httpResponse.getStatus());
      logger.info("========================================");

    } else {
      // Not an HTTP request/response, just continue
      chain.doFilter(request, response);
    }
  }
}
