package nl.koppeltaal.smartserviceregistration.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

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
  @JsonInclude(NON_EMPTY)
  private String publicKey;

  private String name;

  @Column(name = "fhir_store_device_id")
  private String fhirStoreDeviceId;

  @OneToOne
  @JoinColumn(name = "patient_idp", foreignKey = @ForeignKey(name = "patient_idp_fk"))
  private IdentityProvider patientIdp;

  @OneToOne
  @JoinColumn(name = "practitioner_idp", foreignKey = @ForeignKey(name = "practitioner_idp_fk"))
  private IdentityProvider practitionerIdp;

  @ElementCollection
  @CollectionTable(name = "allowed_redirect", joinColumns = @JoinColumn(name = "smart_service_id"))
  @Column(name = "url")
  private Set<String> allowedRedirects = new HashSet<>();

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

  public IdentityProvider getPatientIdp() {
    return patientIdp;
  }

  public void setPatientIdp(IdentityProvider patientIdp) {
    this.patientIdp = patientIdp;
  }

  public IdentityProvider getPractitionerIdp() {
    return practitionerIdp;
  }

  public void setPractitionerIdp(IdentityProvider practitionerIdpEndpoint) {
    this.practitionerIdp = practitionerIdpEndpoint;
  }

  public Set<String> getAllowedRedirects() {
    return allowedRedirects;
  }

  public void setAllowedRedirects(Set<String> allowedRedirects) {
    this.allowedRedirects = allowedRedirects;
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
            ", patientIdp=" + patientIdp +
            ", practitionerIdp=" + practitionerIdp +
            ", allowedRedirects=" + allowedRedirects +
            "} " + super.toString();
  }
}
