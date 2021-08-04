/*
 * Copyright (c) Stichting Koppeltaal 2021.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package nl.koppeltaal.smartserviceregistration.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "permission", uniqueConstraints = {
    @UniqueConstraint(name = "unique_permission", columnNames = {"role_id", "resource_type", "operation"}),
})
public class Permission extends DbEntity {

  @ManyToOne(cascade = CascadeType.REMOVE)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_type")
  private FhirResourceType resourceType;

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

  public FhirResourceType getResourceType() {
    return resourceType;
  }

  public void setResourceType(FhirResourceType resourceType) {
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

  @Override
  public String toString() {
    return "Permission{" +
        "role=" + role +
        ", resourceType=" + resourceType +
        ", operation=" + operation +
        ", scope=" + scope +
        "} " + super.toString();
  }
}
