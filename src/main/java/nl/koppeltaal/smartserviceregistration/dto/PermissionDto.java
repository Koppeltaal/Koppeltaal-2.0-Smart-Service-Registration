/*
 * Copyright (c) Stichting Koppeltaal 2021.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nl.koppeltaal.smartserviceregistration.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import nl.koppeltaal.smartserviceregistration.model.CrudOperation;
import nl.koppeltaal.smartserviceregistration.model.FhirResourceType;
import nl.koppeltaal.smartserviceregistration.model.Permission;
import nl.koppeltaal.smartserviceregistration.model.PermissionScope;
import nl.koppeltaal.smartserviceregistration.model.Role;
import nl.koppeltaal.smartserviceregistration.model.SmartService;

/**
 *
 */
public class PermissionDto {
	private UUID id;
	private Role role;
	private Set<String> grantedDeviceIds = new HashSet<>();
	private Set<UUID> grantedServices = new HashSet<>();
	private FhirResourceType resourceType;
	private CrudOperation operation;
	private PermissionScope scope;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public Set<UUID> getGrantedServices() {
		return grantedServices;
	}

	public void setGrantedServices(Set<UUID> grantedServices) {
		this.grantedServices = grantedServices;
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

	public Set<String> getGrantedDeviceIds() {
		return grantedDeviceIds;
	}

	public void setGrantedDeviceIds(Set<String> grantedDeviceIds) {
		this.grantedDeviceIds = grantedDeviceIds;
	}

	public Permission toPermissionWithoutGrantedServices() {
		final Permission permission = new Permission();

		permission.setId(id);
		permission.setRole(role);
		permission.setResourceType(resourceType);
		permission.setOperation(operation);
		permission.setScope(scope);

		return permission;
	}

	@Override
	public String toString() {
		return "PermissionDto{" +
				"id=" + id +
				", role=" + role +
				", grantedDeviceIds=" + grantedDeviceIds +
				", grantedServices=" + grantedServices +
				", resourceType=" + resourceType +
				", operation=" + operation +
				", scope=" + scope +
				'}';
	}

	public static PermissionDto toPermissionDto(Permission permission) {
		PermissionDto permissionDto = new PermissionDto();

		permissionDto.setId(permission.getId());
		permissionDto.setRole(permission.getRole());
		permissionDto.setResourceType(permission.getResourceType());
		permissionDto.setOperation(permission.getOperation());
		permissionDto.setScope(permission.getScope());
		permissionDto.setGrantedServices(permission.getGrantedServices().stream()
				.map(SmartService::getId).collect(
				Collectors.toSet())
		);
		permissionDto.setGrantedDeviceIds(permission.getGrantedServices().stream()
				.map(SmartService::getFhirStoreDeviceId).collect(
						Collectors.toSet())
		);

		return permissionDto;
	}
}
