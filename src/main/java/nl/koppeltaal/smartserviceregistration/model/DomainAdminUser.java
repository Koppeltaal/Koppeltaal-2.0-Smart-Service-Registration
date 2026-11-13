package nl.koppeltaal.smartserviceregistration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * A user who logged in to domain-admin. Kept to mail the creator of an application, whose Keycloak sub is in
 * {@link DbEntity#getCreatedBy()}.
 */
@Entity
@Table(name = "domain_admin_user")
public class DomainAdminUser {

  @Id
  private String sub;

  private String email;

  @Column(name = "last_login_on")
  private LocalDateTime lastLoginOn;

  protected DomainAdminUser() {
  }

  public DomainAdminUser(String sub) {
    this.sub = sub;
  }

  public String getSub() {
    return sub;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public LocalDateTime getLastLoginOn() {
    return lastLoginOn;
  }

  public void setLastLoginOn(LocalDateTime lastLoginOn) {
    this.lastLoginOn = lastLoginOn;
  }
}
