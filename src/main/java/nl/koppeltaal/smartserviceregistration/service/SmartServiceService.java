package nl.koppeltaal.smartserviceregistration.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;
import java.util.UUID;
import nl.koppeltaal.smartserviceregistration.exception.SmartServiceException;
import nl.koppeltaal.smartserviceregistration.model.Permission;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.repository.PermissionRepository;
import nl.koppeltaal.smartserviceregistration.repository.SmartServiceRepository;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class SmartServiceService {

  private final static Logger LOG = LoggerFactory.getLogger(SmartServiceService.class);

  private final SmartServiceRepository repository;

  private final PermissionRepository permissionRepository;

  public SmartServiceService(SmartServiceRepository repository,
      PermissionRepository permissionRepository) {
    this.repository = repository;
    this.permissionRepository = permissionRepository;
  }

  public Optional<SmartService> findById(UUID id) {
    return repository.findById(id);
  }

  public SmartService registerNewService(String jwksEndpoint, String publicKey, String currentUser) {

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

    smartService.setCreatedBy(currentUser);

    try {
      return repository.save(smartService);
    } catch (DataIntegrityViolationException e) {
      throw new SmartServiceException(jwksEndpoint, jwksEndpoint + " is reeds geregistreerd.", e);
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
        throw new SmartServiceException("",
            String.format("Key needs to be at least 2048 bits but got [%d] instead", bitLength), null);

      return true;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new SmartServiceException("", "Incorrect public key", e);
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
