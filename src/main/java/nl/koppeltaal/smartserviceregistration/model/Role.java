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
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "role")
public class Role extends DbEntity {

  private String name;

  @OneToMany(mappedBy = "role", cascade = CascadeType.REMOVE)
  @OrderBy("resourceType")
  private Set<Permission> permissions = new HashSet<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<Permission> getPermissions() {
    return permissions;
  }

  public void setPermissions(
      Set<Permission> permissions) {
    this.permissions = permissions;
  }

  @Override
  public String toString() {
    return "Role{" +
        "name='" + name + '\'' +
        "} " + super.toString();
  }
}
