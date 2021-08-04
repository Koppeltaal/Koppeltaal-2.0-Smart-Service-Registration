package nl.koppeltaal.smartserviceregistration.exception;

import java.util.UUID;

/**
 * Custom exception that provides the actual role id to properly redirect with error reporting
 */
public class RoleException extends RuntimeException {

  private final UUID roleId;

  public RoleException(UUID roleId, String message, Throwable e) {
    super(message, e);
    this.roleId = roleId;
  }

  public UUID getRoleId() {
    return roleId;
  }
}
