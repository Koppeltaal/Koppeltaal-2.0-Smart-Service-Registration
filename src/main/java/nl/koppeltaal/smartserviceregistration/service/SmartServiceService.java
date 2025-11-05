package nl.koppeltaal.smartserviceregistration.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PostConstruct;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import nl.koppeltaal.smartserviceregistration.exception.SmartServiceRegistrationException;
import nl.koppeltaal.smartserviceregistration.model.CrudOperation;
import nl.koppeltaal.smartserviceregistration.model.Permission;
import nl.koppeltaal.smartserviceregistration.model.PermissionScope;
import nl.koppeltaal.smartserviceregistration.model.Role;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.repository.PermissionRepository;
import nl.koppeltaal.smartserviceregistration.repository.RoleRepository;
import nl.koppeltaal.smartserviceregistration.repository.SmartServiceRepository;
import nl.koppeltaal.spring.boot.starter.smartservice.service.fhir.DeviceFhirClientService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Device.DeviceDeviceNameComponent;
import org.hl7.fhir.r4.model.Device.DeviceNameType;
import org.hl7.fhir.r4.model.Device.FHIRDeviceStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SmartServiceService {

  private final static Logger LOG = LoggerFactory.getLogger(SmartServiceService.class);
  public static final String CLIENT_ID_SYSTEM = "http://vzvz.nl/fhir/NamingSystem/koppeltaal-client-id";
  public static final String CLIENT_ID_PLACEHOLDER = "to_be_replaced_by_device_id";

  private final SmartServiceRepository repository;
  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final DeviceFhirClientService deviceFhirClientService;

  private final String jwksUrl;
  private final String domainAdminInitializeClientId;

  public SmartServiceService(SmartServiceRepository repository,
                             RoleRepository roleRepository,
                             PermissionRepository permissionRepository,
                             DeviceFhirClientService deviceFhirClientService,
                             @Value("${smart.service.init.jwks_url:}") String jwksUrl,
                             @Value("${smart.service.init.client_id:}") String domainAdminInitializeClientId) {
    this.repository = repository;
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.deviceFhirClientService = deviceFhirClientService;
    this.jwksUrl = jwksUrl;
    this.domainAdminInitializeClientId = domainAdminInitializeClientId;
  }

  @PostConstruct
  public void init() {

    if(repository.count() == 0) {
      final SmartService smartService = new SmartService();
      smartService.setName("Smart Registration Service - Domain Admin");
      smartService.setCreatedBy("system");

      smartService.setClientId(
              StringUtils.isNotBlank(domainAdminInitializeClientId) ? domainAdminInitializeClientId : UUID.randomUUID().toString()
      );

      if(StringUtils.isNotBlank(jwksUrl)) {
        try {
          smartService.setJwksEndpoint(new URL(jwksUrl));
        } catch (MalformedURLException e) {
          LOG.error(String.format("Failed to set jwks URL from value [%s]", jwksUrl), e); //continue
        }
      }

      repository.save(smartService);

      //IMPORTANT: Should manually call /authorization/ensure_devices after the client_id is
      //configured in the FHIR store
    }

    if(roleRepository.findByCreatedBy("system").isEmpty()) {
      Role adminRole = new Role();
      adminRole.setCreatedBy("system");
      adminRole.setName("Admin");

      Set<Permission> permissions = new HashSet<>();

      for (ResourceType resourceType : ResourceType.values()) {
        for (CrudOperation operation : CrudOperation.values()) {
          final Permission permission = new Permission();

          permission.setOperation(operation);
          permission.setResourceType(resourceType);
          permission.setScope(PermissionScope.ALL);
          permission.setRole(adminRole);
          permission.setCreatedBy("system");

          permissions.add(permission);
        }
      }

      adminRole.setPermissions(permissions);
      roleRepository.save(adminRole);

      permissionRepository.saveAll(permissions);
    }

    try {
      ensureDevices(); //repair any services missing a Device on server boot
    } catch (Exception e) {
      LOG.error("Failed to ensure Devices on server boot, gracefully continuing", e);
    }
  }


  @Scheduled(fixedDelay = 1000 * 60, initialDelay = 1000 * 60)
  public void ensureDevicesJob() {
    LOG.info("CRON - Ensuring devices");
    ensureDevices();
  }

  public void ensureDevices() {

    Set<SmartService> allByFhirStoreDeviceIdIsNull = repository.findAllByFhirStoreDeviceIdIsNull();

    LOG.info("Found [{}] SMART services without a Device, attempting to ensure", allByFhirStoreDeviceIdIsNull.size());

    allByFhirStoreDeviceIdIsNull.forEach(this::ensureDeviceForASmartService);
  }

  /**
   * Every SMART service has its own client_id. The latest rule is that this equals the Device identifier in FHIR.
   * So the client_id extension is unnecessary, but keeping it for consistency. In the future it might get removed
   */
  private SmartService ensureDeviceForASmartService(SmartService smartService) {
    LOG.info("Attempting to ensure a device for SMART service: {}", smartService);
    final DeviceDeviceNameComponent name = new DeviceDeviceNameComponent();
    name.setName(smartService.getName());
    name.setType(DeviceNameType.USERFRIENDLYNAME);

    Device device = new Device();
    device.setDeviceName(Collections.singletonList(name));
    device.setStatus(FHIRDeviceStatus.ACTIVE);

    final Identifier clientIdIdentifier = new Identifier();
    clientIdIdentifier.setSystem(CLIENT_ID_SYSTEM);
    boolean hasDeviceIdAsClientId = StringUtils.equals(smartService.getFhirStoreDeviceId(), smartService.getClientId());
    clientIdIdentifier.setValue(hasDeviceIdAsClientId ? smartService.getFhirStoreDeviceId() : CLIENT_ID_PLACEHOLDER); //set a placeholder as an identifier is required, replace in an update
    device.addIdentifier(clientIdIdentifier);

    try {
      LOG.info("Attempting to ensure device");
      device = deviceFhirClientService.storeResource(device);
      LOG.info("Ensured device with reference [{}]", device.getIdElement().getIdPart());

      smartService.setFhirStoreDeviceId(device.getIdElement().getIdPart());
      final SmartService savedSmartService = repository.save(smartService);

      LOG.info("Ensured device id on SMART Service: {}", smartService);

      if(!hasDeviceIdAsClientId) {
        LOG.info("Updating SMART Service as the client_id is not the Device id yet: {}", smartService);
        smartService.setClientId(device.getIdElement().getIdPart());
        upsert(smartService);
        repairClientIds();
      }

      return savedSmartService;
    } catch (IOException e) {
      LOG.warn(String.format("Failed to store device for smart service [%s]", smartService), e);
    }

    return null;
  }

  public Optional<SmartService> findById(UUID id) {
    return repository.findById(id);
  }

  private boolean validatePublicKey(SmartService smartService) {
    String cleanPublicKey = smartService.getPublicKey().replace("-----BEGIN PUBLIC KEY-----\r\n", "");
    cleanPublicKey = cleanPublicKey.replace("\r\n-----END PUBLIC KEY-----", "");
    byte[] encoded = Base64.decodeBase64(cleanPublicKey);

    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
    KeyFactory keyFactory;
    try {
      keyFactory = KeyFactory.getInstance("RSA");
      final RSAKey pubKey = (RSAKey) keyFactory.generatePublic(keySpec);
      final int bitLength = pubKey.getModulus().bitLength();

      if(bitLength < 2048)
        throw new SmartServiceRegistrationException(smartService,
            String.format("Key needs to be at least 2048 bits but got [%d] instead", bitLength), null);

      return true;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new SmartServiceRegistrationException(smartService, "Incorrect public key", e);
    }
  }

  public Iterable<SmartService> findAll() {
    return repository.findByOrderByCreatedOnDesc();
  }

  public SmartService updateSmartServiceStatus(UUID id, SmartServiceStatus status, String user) {

    final SmartService smartService = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Unknown id"));

    //WARN: lacking any form of security role check
    LOG.info("User [{}] changed status of SmartService from [{}] to [{}].", user,
        smartService.getStatus(), status);

    smartService.setStatus(status);
    return repository.save(smartService);
  }

  public SmartService updateSmartServiceName(UUID id, String name, String user) {
    final SmartService smartService = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Unknown id"));

    //WARN: lacking any form of security role check
    LOG.info("User [{}] changed status of SmartService from [{}] to [{}].", user,
        smartService.getName(), name);

    smartService.setName(name);
    return repository.save(smartService);
  }

  public SmartService updateSmartServiceRole(UUID id, UUID roleId, String user) {
    final SmartService smartService = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Unknown id"));

    final Role role = roleRepository.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown role id"));

    //WARN: lacking any form of security role check
    LOG.info("User [{}] changed status of SmartService from [{}] to [{}].", user,
        smartService.getRole(), role);

    smartService.setRole(role);
    return repository.save(smartService);
  }

  public SmartService upsert(SmartService smartService) {

    if(StringUtils.isBlank(smartService.getPublicKey())) {
      smartService.setPublicKey(null);
    } else {
      validatePublicKey(smartService);
    }

    if(smartService.getId() != null) {
      SmartService existingSmartService = repository.findById(smartService.getId()).orElseThrow();
      existingSmartService.setJwksEndpoint(smartService.getJwksEndpoint());
      existingSmartService.setName(smartService.getName());
      existingSmartService.setPublicKey(smartService.getPublicKey());
      existingSmartService.setPatientIdp(smartService.getPatientIdp());
      existingSmartService.setPractitionerIdp(smartService.getPractitionerIdp());
      existingSmartService.setRelatedPersonIdp(smartService.getRelatedPersonIdp());
      existingSmartService.setAllowedRedirects(smartService.getAllowedRedirects());
      return repository.save(existingSmartService);
    }

    try {
      return ensureDeviceForASmartService(smartService);  //also saves the smartService
    } catch (DataIntegrityViolationException e) {
      throw new SmartServiceRegistrationException(smartService, smartService.getJwksEndpoint() + " is reeds geregistreerd.", e);
    }
  }

  public void delete(UUID id, String user) {

    final Optional<SmartService> optionalSmartService = repository.findById(id);

    if(optionalSmartService.isEmpty()) {
      LOG.warn("User [{}] tried to delete smart service with id [{}] but it doesn't exist", user, id);
      return;
    }

    final SmartService smartService = optionalSmartService.get();

    LOG.info("User [{}] deleted SmartService {}.", user, smartService);
    repository.delete(smartService);
  }

  /**
   * The KT2 profiles now have a fixed NamingSystem for the client-id.
   * This call repairs identifiers by changing the system to the correct NamingSystem
   */
  public void repairDeviceIdentifiers() {

    AtomicInteger updatedCount = new AtomicInteger(0);
    AtomicInteger failedCount = new AtomicInteger(0);
    AtomicInteger skippedCount = new AtomicInteger(0);

    Iterable<SmartService> allSmartServices = repository.findAll();
    allSmartServices.forEach((smartService -> {

      if(StringUtils.isBlank(smartService.getClientId())) {
        LOG.warn("Skipping SmartService with id [{}] as no client_id is defined", smartService.getId());
        skippedCount.getAndIncrement();
        return;
      }

      Device deviceByClientId = deviceFhirClientService.getResourceByIdentifier(smartService.getClientId(), "https://koppeltaal.nl/client_id", null);
      if(deviceByClientId == null) {
        LOG.info("No Device found for SmartService with client_id [{}] and system [https://koppeltaal.nl/client_id]", smartService.getClientId());
        skippedCount.getAndIncrement();
        return;
      }

      //old records sometimes don't have the status set, this is required
      if(deviceByClientId.hasStatus()) {
        LOG.info("Marking status as active for Device/{}", deviceByClientId.getIdElement().getIdPart());
        deviceByClientId.setStatus(FHIRDeviceStatus.ACTIVE);
      }

      deviceByClientId.getIdentifier().forEach((identifier -> {
        LOG.info("Updating smart-service identifier system with client_id [{}] to [{}]", smartService.getClientId(), CLIENT_ID_SYSTEM);
        identifier.setSystem(CLIENT_ID_SYSTEM);

        try {
          Device device = deviceFhirClientService.storeResource(deviceByClientId);
          LOG.info("Updated system for Device/{}", device.getIdElement().getIdPart());
          updatedCount.getAndIncrement();
        } catch (Exception e) {
          LOG.error("Failed to update Device/{}", deviceByClientId.getIdElement().getIdPart());
          failedCount.getAndIncrement();
        }
      }));
    }));

    LOG.info("Updated [{}] SmartServices", updatedCount.get());
  }

  public void repairClientIds() {
    ICriterion<TokenClientParam> devicesToRepairCriteria = Device.IDENTIFIER.exactly().systemAndIdentifier(CLIENT_ID_SYSTEM, CLIENT_ID_PLACEHOLDER);

    List<Device> resources = deviceFhirClientService.getResources(devicesToRepairCriteria);
    LOG.info("Found [{}] Devices with the client_id placeholder identifier", resources.size());

    for (Device device : resources) {

      for (Identifier identifier : device.getIdentifier()) {
        if(CLIENT_ID_SYSTEM.equals(identifier.getSystem()) && CLIENT_ID_PLACEHOLDER.equals(identifier.getValue())) {
          LOG.info("Transforming identifier for Device/{}", device.getIdElement().getIdPart());
          identifier.setValue(device.getIdElement().getIdPart());
        }
      }

      try {
        deviceFhirClientService.storeResource(device);
        LOG.info("Successfully repaired Device/{}", device.getIdElement().getIdPart());
      } catch (Exception e) {
        LOG.error("Failed to update Device.", e);
      }

    }
  }
}
