package nl.koppeltaal.smartserviceregistration.model;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

  @Override
  public String toString() {
    return "SmartService{" +
        "role=" + role +
        ", clientId='" + clientId + '\'' +
        ", status=" + status +
        ", jwksEndpoint=" + jwksEndpoint +
        ", publicKey='" + publicKey + '\'' +
        ", name='" + name + '\'' +
        "} " + super.toString();
  }
}
