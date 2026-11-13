package nl.koppeltaal.smartserviceregistration.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Every writer saves whole entities, so a stale copy must not overwrite a deactivation. */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED) // each repository call commits on its own, like the services
class SmartServiceVersionTest {

  @Autowired private SmartServiceRepository repository;

  @AfterEach
  void cleanUp() {
    repository.deleteAll();
  }

  @Test
  void aStaleCopyCannotOverwriteADeactivation() {
    SmartService approved = new SmartService();
    approved.setName("Test App");
    approved.setStatus(SmartServiceStatus.APPROVED);
    UUID id = repository.save(approved).getId();
    SmartService stale = repository.findById(id).orElseThrow();
    SmartService current = repository.findById(id).orElseThrow();

    current.setStatus(SmartServiceStatus.DEACTIVATED);
    repository.save(current);
    stale.setName("Renamed");

    assertThatThrownBy(() -> repository.save(stale)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(SmartServiceStatus.DEACTIVATED);
  }
}
