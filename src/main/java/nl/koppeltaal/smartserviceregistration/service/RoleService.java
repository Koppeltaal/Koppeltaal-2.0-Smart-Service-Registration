package nl.koppeltaal.smartserviceregistration.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import nl.koppeltaal.smartserviceregistration.dto.PermissionDto;
import nl.koppeltaal.smartserviceregistration.exception.PermissionException;
import nl.koppeltaal.smartserviceregistration.model.Permission;
import nl.koppeltaal.smartserviceregistration.model.Role;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.repository.PermissionRepository;
import nl.koppeltaal.smartserviceregistration.repository.RoleRepository;
import nl.koppeltaal.smartserviceregistration.repository.SmartServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

  private final static Logger LOG = LoggerFactory.getLogger(RoleService.class);

  private final PermissionRepository permissionRepository;
  private final RoleRepository repository;
  private final SmartServiceRepository smartServiceRepository;

  public RoleService(
      PermissionRepository permissionRepository,
      RoleRepository repository,
      SmartServiceRepository smartServiceRepository) {
    this.permissionRepository = permissionRepository;
    this.repository = repository;
    this.smartServiceRepository = smartServiceRepository;
  }

  public List<Role> findAll() {
    return repository.findAll();
  }

  public Optional<Role> findById(UUID roleId) {
    return repository.findById(roleId);
  }

  public Role save(Role role) {
    return repository.save(role);
  }

  public void addOrUpdatePermission(PermissionDto permissionDto, UUID roleId) {

    try {
      Permission newPermission = getNewPermission(permissionDto, roleId);

      final Set<UUID> grantedServicesIds = permissionDto.getGrantedServices();
      if(!grantedServicesIds.isEmpty()) {
        final Set<SmartService> grantedServices = smartServiceRepository.findAllById(grantedServicesIds);
        newPermission.setGrantedServices(grantedServices);
      }

      permissionRepository.save(newPermission);
    } catch (DataIntegrityViolationException e) {
      throw new PermissionException(roleId, String.format(
          "Er bestaat al een permissie voor resource [%s] en operation [%s]. Deze combinatie moet uniek zijn.",
          permissionDto.getResourceType(), permissionDto.getOperation()), e, permissionDto.getId());
    } catch (Exception e) {
      throw new PermissionException(roleId, e.getMessage(), e, permissionDto.getId());
    }
  }

  private Permission getNewPermission(PermissionDto permissionDto, UUID roleId) {

    Permission newPermission = permissionDto.toPermissionWithoutGrantedServices();

    // If it's an update, apply new fields that are allowed to change onto the previous version
    if(permissionDto.getId() != null) {
      final Permission previousPermission = permissionRepository.findById(permissionDto.getId())
          .orElseThrow(() -> new IllegalArgumentException(
              String.format("Cannot update permission with id [%s], not found",
                  permissionDto.getId())));

      previousPermission.setScope(newPermission.getScope());
      previousPermission.setResourceType(newPermission.getResourceType());
      previousPermission.setOperation(newPermission.getOperation());

      newPermission = previousPermission;
    } else {
      // if it's a new permission, set the role
      final Role role = repository.findById(roleId)
          .orElseThrow(() -> new IllegalArgumentException("Unknown role id"));
      newPermission.setRole(role);
    }

    return newPermission;
  }

  public void deletePermission(UUID permissionId) {
    permissionRepository.deleteById(permissionId);
  }
}
