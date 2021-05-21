package nl.koppeltaal.smartserviceregistration.model;

import com.sun.istack.NotNull;
import java.net.URL;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * This entity resembles a <a href="https://hl7.org/fhir/uv/bulkdata/authorization/index.html">SMART Backend Service</a>.
 */
@Entity
@Table(name = "smart_service", uniqueConstraints = {
    @UniqueConstraint(name = "unique_jwks_endpoint", columnNames = "jwks_endpoint")
}, indexes = {
    @Index(name = "client_id_index", columnList = "client_id", unique = true)
})
public class SmartService extends DbEntity {

  @Column(name = "client_id")
  private String clientId = UUID.randomUUID().toString();

  private SmartServiceStatus status = SmartServiceStatus.PENDING;

  @NotNull
  @Column(name = "jwks_endpoint")
  private URL jwksEndpoint;

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

  @Override
  public String toString() {
    return "SmartService{" +
        "clientId='" + clientId + '\'' +
        ", status=" + status +
        ", jwksEndpoint=" + jwksEndpoint +
        "} " + super.toString();
  }
}
