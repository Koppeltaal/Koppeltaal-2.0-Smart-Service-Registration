package nl.koppeltaal.smartserviceregistration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.repository.PermissionRepository;
import nl.koppeltaal.smartserviceregistration.repository.RoleRepository;
import nl.koppeltaal.smartserviceregistration.repository.SmartServiceRepository;
import nl.koppeltaal.smartserviceregistration.repository.DeactivationDetectionRepository;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService.DeactivationResult;
import nl.koppeltaal.spring.boot.starter.smartservice.service.fhir.DeviceFhirClientService;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Device.FHIRDeviceStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A detection is stored together with its effect or not at all: if the deactivation fails at commit, a stored detection
 * would make every retry skip the deactivation.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED) // the service manages its own transaction
class DeactivationTransactionTest {

  private static final String CLIENT_ID = "11111111-2222-3333-4444-555555555555";
  private static final String DETECTION = "6c3b9b8e-1d1f-4c55-9d43-1f0c2f3a9b10";

  @Autowired private SmartServiceRepository repository;
  @Autowired private DeactivationDetectionRepository deactivationDetectionRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PermissionRepository permissionRepository;
  @Autowired private PlatformTransactionManager transactionManager;

  private final DeviceFhirClientService deviceFhirClientService = mock(DeviceFhirClientService.class);
  private final AtomicInteger conflictsToCause = new AtomicInteger();
  private SmartServiceService service;

  @BeforeEach
  void setUp() {
    SmartService approved = new SmartService();
    approved.setName("Test App");
    approved.setClientId(CLIENT_ID);
    approved.setFhirStoreDeviceId(CLIENT_ID);
    approved.setStatus(SmartServiceStatus.APPROVED);
    repository.save(approved);
    service = new SmartServiceService(repository, deactivationDetectionRepository, roleRepository, permissionRepository,
        deviceFhirClientService, conflictingBeforeCommit(), new SuspensionAdmins(""), "", "");
  }

  @AfterEach
  void cleanUp() {
    deactivationDetectionRepository.deleteAll();
    repository.deleteAll();
  }

  /** Runs the work in a real transaction; before it commits, another transaction may change the same service. */
  private TransactionOperations conflictingBeforeCommit() {
    TransactionTemplate work = new TransactionTemplate(transactionManager);
    TransactionTemplate meanwhile = new TransactionTemplate(transactionManager);
    meanwhile.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return new TransactionOperations() {
      @Override
      public <T> T execute(TransactionCallback<T> action) {
        return work.execute(status -> {
          T result = action.doInTransaction(status);
          if (conflictsToCause.getAndDecrement() > 0) {
            meanwhile.executeWithoutResult(other -> {
              SmartService renamed = repository.findByClientId(CLIENT_ID).orElseThrow();
              renamed.setName("Renamed meanwhile");
              repository.save(renamed);
            });
          }
          return result;
        });
      }
    };
  }

  @Test
  void aConflictAtCommitStoresNeitherTheDetectionNorTheDeactivationAndTheRetrySucceeds() throws Exception {
    conflictsToCause.set(1);

    assertThatThrownBy(() -> service.deactivate(CLIENT_ID, DETECTION, "Rejected 300 write requests"))
        .isInstanceOf(OptimisticLockingFailureException.class);

    assertThat(deactivationDetectionRepository.existsById(DETECTION)).isFalse();
    assertThat(repository.findByClientId(CLIENT_ID).orElseThrow().getStatus()).isEqualTo(SmartServiceStatus.APPROVED);
    verifyNoInteractions(deviceFhirClientService);

    Device device = new Device();
    device.setId(CLIENT_ID);
    device.setStatus(FHIRDeviceStatus.ACTIVE);
    when(deviceFhirClientService.getResourceByReference(CLIENT_ID)).thenReturn(device);

    DeactivationResult retry = service.deactivate(CLIENT_ID, DETECTION, "Rejected 300 write requests").orElseThrow();

    assertThat(retry.newlyDeactivated()).isTrue();
    assertThat(retry.confirmed()).isTrue();
    assertThat(deactivationDetectionRepository.existsById(DETECTION)).isTrue();
    assertThat(repository.findByClientId(CLIENT_ID).orElseThrow().getStatus()).isEqualTo(SmartServiceStatus.DEACTIVATED);
    verify(deviceFhirClientService).storeResource(device);
  }
}
