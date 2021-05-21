package nl.koppeltaal.smartserviceregistration.exception;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * Custom exception that provides the actual endpoint to the templating  enginge for error reporting
 * and proper UI restoring of the value
 */
public class DuplicateJwksEndpointException extends RuntimeException {

  private final String endpoint;

  public DuplicateJwksEndpointException(String endpoint, String message, DataIntegrityViolationException e) {
    super(message, e);
    this.endpoint = endpoint;
  }

  public String getEndpoint() {
    return endpoint;
  }
}
