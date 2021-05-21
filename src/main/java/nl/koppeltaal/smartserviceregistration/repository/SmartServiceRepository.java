package nl.koppeltaal.smartserviceregistration.repository;

import java.util.List;
import java.util.UUID;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import org.springframework.data.repository.CrudRepository;

public interface SmartServiceRepository extends CrudRepository<SmartService, UUID> {
  List<SmartService> findByOrderByCreatedOnDesc();
}
