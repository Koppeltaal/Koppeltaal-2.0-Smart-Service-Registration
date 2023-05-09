package nl.koppeltaal.smartserviceregistration.model;

import java.net.URL;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * This entity resembles a <a href="https://hl7.org/fhir/uv/bulkdata/authorization/index.html">SMART Backend Service</a>.
 */
@Entity
@Table(name = "smart_service", uniqueConstraints = {
    @UniqueConstraint(name = "unique_jwks_endpoint", columnNames = "jwks_endpoint"),
    @UniqueConstraint(name = "unique_public_key", columnNames = "public_key")
}, indexes = {
    @Index(name = "client_id_index", columnList = "client_id", unique = true)
})
public class SmartService extends DbEntity {

  @ManyToOne
  @JoinColumn(name = "role_id")
  private Role role;

  @Column(name = "client_id")
  private String clientId = UUID.randomUUID().toString();

  @Enumerated(EnumType.STRING)
  private SmartServiceStatus status = SmartServiceStatus.PENDING;

  @Column(name = "jwks_endpoint")
  private URL jwksEndpoint;

  @Column(name = "public_key", length = 512)
  private String publicKey;

  private String name;

  @Column(name = "fhir_store_device_id")
  private String fhirStoreDeviceId;

  @Column(name = "patient_idp_endpoint")
  private String patientIdpEndpoint;

  @Column(name = "practitioner_idp_endpoint")
  private String practitionerIdpEndpoint;

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public SmartServiceStatus getStatus() {
    return status;
  }

  public void setStatus(SmartServiceStatus status) {
    this.status = status;
  }

  public URL getJwksEndpoint() {
    return jwksEndpoint;
  }

  public void setJwksEndpoint(URL jwksEndpoint) {
    this.jwksEndpoint = jwksEndpoint;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFhirStoreDeviceId() {
    return fhirStoreDeviceId;
  }

  public void setFhirStoreDeviceId(String fhirStoreDeviceId) {
    this.fhirStoreDeviceId = fhirStoreDeviceId;
  }

  public String getPatientIdpEndpoint() {
    return patientIdpEndpoint;
  }

  public void setPatientIdpEndpoint(String patientIdpEndpoint) {
    this.patientIdpEndpoint = patientIdpEndpoint;
  }

  public String getPractitionerIdpEndpoint() {
    return practitionerIdpEndpoint;
  }

  public void setPractitionerIdpEndpoint(String practitionerIdpEndpoint) {
    this.practitionerIdpEndpoint = practitionerIdpEndpoint;
  }

  @Override
  public String toString() {
    return "SmartService{" +
            "role=" + role +
            ", clientId='" + clientId + '\'' +
            ", status=" + status +
            ", jwksEndpoint=" + jwksEndpoint +
            ", publicKey='" + publicKey + '\'' +
            ", name='" + name + '\'' +
            ", fhirStoreDeviceId='" + fhirStoreDeviceId + '\'' +
            ", patientIdpEndpoint='" + patientIdpEndpoint + '\'' +
            ", practitionerIdpEndpoint='" + practitionerIdpEndpoint + '\'' +
            "} " + super.toString();
  }
}
