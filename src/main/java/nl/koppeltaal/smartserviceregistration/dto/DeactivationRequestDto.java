package nl.koppeltaal.smartserviceregistration.dto;

import nl.koppeltaal.smartserviceregistration.model.DeactivationMode;

public class DeactivationRequestDto {

  private String clientId;
  private DeactivationMode mode;
  private String detectionId;
  private String reason;

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public DeactivationMode getMode() {
    return mode;
  }

  public void setMode(DeactivationMode mode) {
    this.mode = mode;
  }

  public String getDetectionId() {
    return detectionId;
  }

  public void setDetectionId(String detectionId) {
    this.detectionId = detectionId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
