package nl.koppeltaal.smartserviceregistration.dto;

import java.util.List;

public class AuthorizationDto {
  private final String clientId;
  private final List<PermissionDto> permissions;

  public AuthorizationDto(String clientId, List<PermissionDto> permissions) {
    this.clientId = clientId;
    this.permissions = permissions;
  }

  public String getClientId() {
    return clientId;
  }

  public List<PermissionDto> getPermissions() {
    return permissions;
  }
}
