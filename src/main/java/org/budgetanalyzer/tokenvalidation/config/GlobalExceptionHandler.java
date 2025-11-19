package org.budgetanalyzer.tokenvalidation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global exception handler to catch and log any errors that occur during request processing.
 *
 * <p>This is especially useful for debugging 400 Bad Request errors that might be returned before
 * reaching the controller.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Catch 404 errors (endpoint not found). */
  @ExceptionHandler(NoHandlerFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<String> handleNotFound(NoHandlerFoundException ex) {
    logger.error("========================================");
    logger.error("404 NOT FOUND ERROR");
    logger.error("========================================");
    logger.error("Requested URL: {}", ex.getRequestURL());
    logger.error("HTTP Method: {}", ex.getHttpMethod());
    logger.error("Handler not found");
    logger.error("========================================");

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
  }

  /** Catch ALL other exceptions. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleAllExceptions(Exception ex) {
    logger.error("========================================");
    logger.error("EXCEPTION CAUGHT IN GLOBAL HANDLER");
    logger.error("========================================");
    logger.error("Exception Type: {}", ex.getClass().getName());
    logger.error("Exception Message: {}", ex.getMessage());
    logger.error("Stack Trace:", ex);
    logger.error("========================================");

    // Return 500 for most errors
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
  }

  /** Catch IllegalArgumentException (which might cause 400 errors). */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
    logger.error("========================================");
    logger.error("400 BAD REQUEST - IllegalArgumentException");
    logger.error("========================================");
    logger.error("Exception Message: {}", ex.getMessage());
    logger.error("Stack Trace:", ex);
    logger.error("========================================");

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad Request");
  }
}
