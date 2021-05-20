package nl.koppeltaal.smartserviceregistration.service;

import java.net.URL;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.repository.SmartServiceRepository;
import org.springframework.stereotype.Service;

@Service
public class RegisterSmartServiceService {

  private final SmartServiceRepository repository;

  public RegisterSmartServiceService(SmartServiceRepository repository) {
    this.repository = repository;
  }

  public SmartService registerNewService(URL jwksEndpoint, String currentUser) {

    final SmartService smartService = new SmartService();

    smartService.setJwksEndpoint(jwksEndpoint);
    smartService.setCreatedBy(currentUser);

    return repository.save(smartService);
  }
}
