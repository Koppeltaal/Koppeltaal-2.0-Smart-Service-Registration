package nl.koppeltaal.smartserviceregistration.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import nl.koppeltaal.smartserviceregistration.exception.SmartServiceRegistrationException;
import nl.koppeltaal.smartserviceregistration.model.Role;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.repository.RoleRepository;
import nl.koppeltaal.smartserviceregistration.repository.SmartServiceRepository;
import nl.koppeltaal.spring.boot.starter.smartservice.service.fhir.DeviceFhirClientService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Device.DeviceDeviceNameComponent;
import org.hl7.fhir.r4.model.Device.FHIRDeviceStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class SmartServiceService {

  private final static Logger LOG = LoggerFactory.getLogger(SmartServiceService.class);

  private final SmartServiceRepository repository;
  private final RoleRepository roleRepository;
  private final DeviceFhirClientService deviceFhirClientService;

  public SmartServiceService(SmartServiceRepository repository,
      RoleRepository roleRepository,
      DeviceFhirClientService deviceFhirClientService) {
    this.repository = repository;
    this.roleRepository = roleRepository;
    this.deviceFhirClientService = deviceFhirClientService;
  }

  @PostConstruct
  public void ensureDevices() {

    repository.findAllByFhirStoreDeviceIdIsNull()
        .forEach(this::ensureDeviceForASmartService);
  }

  /**
   * Every SMART service has its own client_id. The client_id could change over time, but the application
   * itself should remain consistent. Therefore, whenever a SMART service is created, the service pushes
   * a Device record to the FHIR store. The Device has the client_id set as an identifier.
   */
  private SmartService ensureDeviceForASmartService(SmartService smartService) {
    final DeviceDeviceNameComponent name = new DeviceDeviceNameComponent();
    name.setName(smartService.getName());

    Device device = new Device();
    device.setDeviceName(Collections.singletonList(name));
    device.setStatus(FHIRDeviceStatus.ACTIVE);

    final Identifier clientIdIdentifier = new Identifier();
    clientIdIdentifier.setSystem("https://koppeltaal.nl/client_id");
    clientIdIdentifier.setValue(smartService.getClientId());
    device.addIdentifier(clientIdIdentifier);

    try {
      device = deviceFhirClientService.storeResource(device);

      smartService.setFhirStoreDeviceId(device.getIdElement().toUnqualifiedVersionless().getValue());
      return repository.save(smartService);
    } catch (IOException e) {
      LOG.warn("Failed to store device for smart service [{}]", smartService);
    }

    return null;
  }

  public Optional<SmartService> findById(UUID id) {
    return repository.findById(id);
  }

  public SmartService registerNewService(String jwksEndpoint, String name, String publicKey, String currentUser) {

    final SmartService smartService = new SmartService();

    if(StringUtils.isNotBlank(jwksEndpoint)) {
      try {
        smartService.setJwksEndpoint(new URL(jwksEndpoint));
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    if(StringUtils.isNotBlank(publicKey) &&  isValidPublicKey(publicKey)) {
      smartService.setPublicKey(publicKey);
    }

    smartService.setName(name);
    smartService.setCreatedBy(currentUser);

    try {
      return ensureDeviceForASmartService(smartService);  //also saves the smartService
    } catch (DataIntegrityViolationException e) {
      throw new SmartServiceRegistrationException(jwksEndpoint, jwksEndpoint + " is reeds geregistreerd.", e);
    }
  }

  private boolean isValidPublicKey(String publicKey) {
    String cleanPublicKey = publicKey.replace("-----BEGIN PUBLIC KEY-----\r\n", "");
    cleanPublicKey = cleanPublicKey.replace("\r\n-----END PUBLIC KEY-----", "");
    byte[] encoded = Base64.decodeBase64(cleanPublicKey);

    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
    KeyFactory keyFactory;
    try {
      keyFactory = KeyFactory.getInstance("RSA");
      final RSAKey pubKey = (RSAKey) keyFactory.generatePublic(keySpec);
      final int bitLength = pubKey.getModulus().bitLength();

      if(bitLength < 2048)
        throw new SmartServiceRegistrationException("",
            String.format("Key needs to be at least 2048 bits but got [%d] instead", bitLength), null);

      return true;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new SmartServiceRegistrationException("", "Incorrect public key", e);
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
}
