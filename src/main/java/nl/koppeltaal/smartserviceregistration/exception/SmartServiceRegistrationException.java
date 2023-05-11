package nl.koppeltaal.smartserviceregistration.exception;

import nl.koppeltaal.smartserviceregistration.model.SmartService;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Custom exception that provides the actual endpoint to the templating  engine for error reporting
 * and proper UI restoring of the value
 */
public class SmartServiceRegistrationException extends RuntimeException {

  private final SmartService smartService;

  public SmartServiceRegistrationException(SmartService smartService, String message, Throwable e) {
    super(message, e);
    this.smartService = smartService;
  }

  public SmartService getSmartService() {
    return smartService;
  }

  public String getEndpoint() {
    return smartService.getJwksEndpoint() != null ? smartService.getJwksEndpoint().toString() : null;
  }
}
