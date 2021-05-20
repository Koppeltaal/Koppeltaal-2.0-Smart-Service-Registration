package nl.koppeltaal.smartserviceregistration.service;

import java.net.URL;
import java.util.UUID;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.repository.SmartServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmartServiceService {

  private final static Logger LOG = LoggerFactory.getLogger(SmartServiceService.class);

  private final SmartServiceRepository repository;

  public SmartServiceService(SmartServiceRepository repository) {
    this.repository = repository;
  }

  public SmartService registerNewService(URL jwksEndpoint, String currentUser) {

    final SmartService smartService = new SmartService();

    smartService.setJwksEndpoint(jwksEndpoint);
    smartService.setCreatedBy(currentUser);

    return repository.save(smartService);
  }

  public Iterable<SmartService> findAll() {
    return repository.findAll();
  }

  public SmartService updateSmartServiceStatus(UUID id, SmartServiceStatus status, String user) {

    final SmartService smartService = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Unknown id"));

    //WARN: lacking any form of security check
    LOG.info("User [{}] changed status of SmartService from [{}] to [{}].", user,
        smartService.getStatus(), status);

    smartService.setStatus(status);
    return repository.save(smartService);
  }
}
