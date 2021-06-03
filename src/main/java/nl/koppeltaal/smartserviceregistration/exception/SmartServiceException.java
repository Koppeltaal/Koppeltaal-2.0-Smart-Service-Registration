package nl.koppeltaal.smartserviceregistration.exception;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * Custom exception that provides the actual endpoint to the templating  engine for error reporting
 * and proper UI restoring of the value
 */
public class SmartServiceException extends RuntimeException {

  private final String endpoint;

  public SmartServiceException(String endpoint, String message, Throwable e) {
    super(message, e);
    this.endpoint = endpoint;
  }

  public String getEndpoint() {
    return endpoint;
  }
}
