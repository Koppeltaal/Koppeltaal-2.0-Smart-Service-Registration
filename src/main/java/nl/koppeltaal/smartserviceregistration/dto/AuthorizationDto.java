package nl.koppeltaal.smartserviceregistration.dto;

import java.util.List;

public class AuthorizationDto {
  private final String clientId;
  private final String deviceReference;
  private final List<PermissionDto> permissions;

  public AuthorizationDto(String clientId, String deviceReference,
      List<PermissionDto> permissions) {
    this.clientId = clientId;
    this.deviceReference = deviceReference;
    this.permissions = permissions;
  }

  public String getClientId() {
    return clientId;
  }

  public List<PermissionDto> getPermissions() {
    return permissions;
  }

  public String getDeviceReference() {
    return deviceReference;
  }
}
