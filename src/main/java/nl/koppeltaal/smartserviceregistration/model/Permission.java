/*
 * Copyright (c) Stichting Koppeltaal 2021.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package nl.koppeltaal.smartserviceregistration.model;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hl7.fhir.r4.model.ResourceType;

@Entity
@Table(name = "permission", uniqueConstraints = {
    @UniqueConstraint(name = "unique_permission", columnNames = {"role_id", "resource_type", "operation"}),
})
public class Permission extends DbEntity {

  @ManyToOne
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @ManyToMany(cascade = CascadeType.ALL)
  @JoinTable(
      name = "permission_service_grant",
      joinColumns = @JoinColumn(name="permission_id"),
      inverseJoinColumns = @JoinColumn(name = "smart_service_id")
  )
  private Set<SmartService> grantedServices = new HashSet<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_type")
  private ResourceType resourceType;

  @Enumerated(EnumType.STRING)
  private CrudOperation operation;

  @Enumerated(EnumType.STRING)
  private PermissionScope scope;

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public ResourceType getResourceType() {
    return resourceType;
  }

  public void setResourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
  }

  public CrudOperation getOperation() {
    return operation;
  }

  public void setOperation(CrudOperation operation) {
    this.operation = operation;
  }

  public PermissionScope getScope() {
    return scope;
  }

  public void setScope(PermissionScope scope) {
    this.scope = scope;
  }

  public Set<SmartService> getGrantedServices() {
    return grantedServices;
  }

  public void setGrantedServices(Set<SmartService> grantedServices) {
    this.grantedServices = grantedServices;
  }

  @Override
  public String toString() {
    return "Permission{" +
        "role=" + role +
        ", grantedServices=" + grantedServices +
        ", resourceType=" + resourceType +
        ", operation=" + operation +
        ", scope=" + scope +
        "} " + super.toString();
  }
}
