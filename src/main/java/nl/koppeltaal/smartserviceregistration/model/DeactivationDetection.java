package nl.koppeltaal.smartserviceregistration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * A detection the FHIR server reported in enforce mode. Only its first delivery may change a status: a retry, or an
 * older delivery that arrives late, must not deactivate a client that was approved again.
 */
@Entity
@Table(name = "deactivation_detection")
public class DeactivationDetection {

  public static final int MAX_ID_LENGTH = 64;

  @Id
  @Column(name = "detection_id", length = MAX_ID_LENGTH)
  private String detectionId;

  @Column(name = "client_id", nullable = false)
  private String clientId;

  @Column(name = "handled_on", nullable = false)
  private LocalDateTime handledOn;

  protected DeactivationDetection() {
  }

  public DeactivationDetection(String detectionId, String clientId) {
    this.detectionId = detectionId;
    this.clientId = clientId;
    this.handledOn = LocalDateTime.now();
  }

  public String getDetectionId() {
    return detectionId;
  }

  public String getClientId() {
    return clientId;
  }

  public LocalDateTime getHandledOn() {
    return handledOn;
  }
}
