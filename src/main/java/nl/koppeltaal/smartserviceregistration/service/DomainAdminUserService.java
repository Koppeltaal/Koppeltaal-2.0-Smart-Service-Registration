package nl.koppeltaal.smartserviceregistration.service;

import java.time.LocalDateTime;
import java.util.Optional;
import nl.koppeltaal.smartserviceregistration.model.DomainAdminUser;
import nl.koppeltaal.smartserviceregistration.repository.DomainAdminUserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class DomainAdminUserService {

  private final DomainAdminUserRepository repository;

  public DomainAdminUserService(DomainAdminUserRepository repository) {
    this.repository = repository;
  }

  /**
   * @param identifier the email claim of the ID token, or the sub when the token has none
   */
  public void recordLogin(String sub, String identifier) {
    if (StringUtils.isBlank(sub)) {
      return;
    }
    final DomainAdminUser user = repository.findById(sub).orElseGet(() -> new DomainAdminUser(sub));
    if (StringUtils.isNotBlank(identifier) && !identifier.equals(sub)) {
      user.setEmail(identifier);
    }
    user.setLastLoginOn(LocalDateTime.now());
    repository.save(user);
  }

  public Optional<String> findEmail(String sub) {
    if (StringUtils.isBlank(sub)) {
      return Optional.empty();
    }
    return repository.findById(sub).map(DomainAdminUser::getEmail).filter(StringUtils::isNotBlank);
  }
}
