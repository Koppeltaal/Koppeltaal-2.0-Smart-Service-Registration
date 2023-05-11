package nl.koppeltaal.smartserviceregistration.repository;

import nl.koppeltaal.smartserviceregistration.model.IdentityProvider;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface IdentityProviderRepository extends CrudRepository<IdentityProvider, UUID> {
  List<IdentityProvider> findByOrderByCreatedOnDesc();
}
