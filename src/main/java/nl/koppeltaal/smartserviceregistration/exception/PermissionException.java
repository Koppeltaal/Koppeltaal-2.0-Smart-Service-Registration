package nl.koppeltaal.smartserviceregistration.exception;

import java.util.UUID;

/**
 * Custom exception that provides the actual role id to properly redirect with error reporting
 */
public class PermissionException extends RuntimeException {

  private final UUID roleId;
  private final UUID permissionId;

  public PermissionException(UUID roleId, String message, Throwable e, UUID permissionId) {
    super(message, e);
    this.roleId = roleId;
    this.permissionId = permissionId;
  }

  public UUID getRoleId() {
    return roleId;
  }

  public UUID getPermissionId() {
    return permissionId;
  }

}
