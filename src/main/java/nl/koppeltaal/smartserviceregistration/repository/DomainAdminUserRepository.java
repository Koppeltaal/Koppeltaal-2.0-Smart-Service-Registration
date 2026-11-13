package nl.koppeltaal.smartserviceregistration.repository;

import nl.koppeltaal.smartserviceregistration.model.DomainAdminUser;
import org.springframework.data.repository.CrudRepository;

public interface DomainAdminUserRepository extends CrudRepository<DomainAdminUser, String> {
}
