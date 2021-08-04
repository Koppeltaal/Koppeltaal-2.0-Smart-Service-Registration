package nl.koppeltaal.smartserviceregistration.service;

import java.util.Optional;
import java.util.UUID;
import nl.koppeltaal.smartserviceregistration.exception.RoleException;
import nl.koppeltaal.smartserviceregistration.model.Permission;
import nl.koppeltaal.smartserviceregistration.model.Role;
import nl.koppeltaal.smartserviceregistration.repository.PermissionRepository;
import nl.koppeltaal.smartserviceregistration.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

  private final static Logger LOG = LoggerFactory.getLogger(RoleService.class);

  private final PermissionRepository permissionRepository;

  private final RoleRepository repository;

  public RoleService(
      PermissionRepository permissionRepository,
      RoleRepository repository) {
    this.permissionRepository = permissionRepository;
    this.repository = repository;
  }


  public Iterable<Role> findAll() {
    return repository.findAll();
  }

  public Optional<Role> findById(UUID roleId) {
    return repository.findById(roleId);
  }

  public Role save(Role role) {
    return repository.save(role);
  }

  public void addPermission(Permission permission, UUID roleId) {

    try {
      final Role role = repository.findById(roleId)
          .orElseThrow(() -> new IllegalArgumentException("Unknown role id"));
      permission.setRole(role);

      permissionRepository.save(permission);

      repository.save(role);
    } catch (DataIntegrityViolationException e) {
      throw new RoleException(roleId, String.format(
          "Er bestaat al een permissie voor resource [%s] en operation [%s]. Deze combinatie moet uniek zijn.",
          permission.getResourceType(), permission.getOperation()), e);
    } catch (Exception e) {
      throw new RoleException(roleId, e.getMessage(), e);
    }
  }
}
