package nl.koppeltaal.smartserviceregistration.dto;

import java.util.List;

public class AuthorizationDto {
  private final String clientId;
  private final String deviceId;
  private final List<PermissionDto> permissions;

  public AuthorizationDto(String clientId, String deviceId, List<PermissionDto> permissions) {
    this.clientId = clientId;
    this.deviceId = deviceId;
    this.permissions = permissions;
  }

  public String getClientId() {
    return clientId;
  }

  public List<PermissionDto> getPermissions() {
    return permissions;
  }

  public String getDeviceId() {
    return deviceId;
  }
}
