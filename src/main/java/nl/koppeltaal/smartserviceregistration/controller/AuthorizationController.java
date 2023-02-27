package nl.koppeltaal.smartserviceregistration.controller;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import nl.koppeltaal.smartserviceregistration.dto.AuthorizationDto;
import nl.koppeltaal.smartserviceregistration.dto.PermissionDto;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("authorization")
public class AuthorizationController {

  private final SmartServiceService smartServiceService;

  public AuthorizationController(SmartServiceService smartServiceService) {
    this.smartServiceService = smartServiceService;
  }

  @GetMapping
  public List<AuthorizationDto> getAuthorizations() {

    final Iterable<SmartService> smartServices = smartServiceService.findAll();

    final List<AuthorizationDto> result = StreamSupport.stream(smartServices.spliterator(), false)
        .filter(smartService -> smartService.getRole() != null)
        .map((smartService -> {
          final List<PermissionDto> permissionDtos = smartService.getRole().getPermissions()
              .stream()
              .map((permission -> {
                final PermissionDto permissionDto = PermissionDto.toPermissionDto(permission);
                permissionDto.setRole(null);
                return permissionDto;
              }))
              .collect(Collectors.toList());

          return new AuthorizationDto(smartService.getClientId(), smartService.getFhirStoreDeviceId(), permissionDtos);
        }))
        .collect(Collectors.toList());
    return result;
  }

  @PostMapping("ensure_devices")
  public void ensureDevices()  {
    smartServiceService.ensureDevices();
  }

@PostMapping("repair_device_identifiers")
  public void repairDeviceIdentifiers()  {
    smartServiceService.repairDeviceIdentifiers();
  }

}
