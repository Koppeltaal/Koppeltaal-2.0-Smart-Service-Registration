package nl.koppeltaal.smartserviceregistration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import nl.koppeltaal.smartserviceregistration.exception.SmartServiceRegistrationException;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.model.DeactivationDetection;
import nl.koppeltaal.smartserviceregistration.repository.PermissionRepository;
import nl.koppeltaal.smartserviceregistration.repository.RoleRepository;
import nl.koppeltaal.smartserviceregistration.repository.SmartServiceRepository;
import nl.koppeltaal.smartserviceregistration.repository.DeactivationDetectionRepository;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService.DeactivationResult;
import nl.koppeltaal.spring.boot.starter.smartservice.service.fhir.DeviceFhirClientService;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Device.FHIRDeviceStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SmartServiceServiceTest {

  private static final String CLIENT_ID = "11111111-2222-3333-4444-555555555555";
  private static final String OTHER_CLIENT_ID = "99999999-8888-7777-6666-555555555555";
  private static final String DETECTION = "6c3b9b8e-1d1f-4c55-9d43-1f0c2f3a9b10";
  private static final String ADMIN = "admin@example.com";

  @Mock private SmartServiceRepository repository;
  @Mock private DeactivationDetectionRepository deactivationDetectionRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PermissionRepository permissionRepository;
  @Mock private DeviceFhirClientService deviceFhirClientService;

  private final Set<String> handledDetections = new HashSet<>();
  private final ListAppender<ILoggingEvent> serviceLog = new ListAppender<>();
  private SmartServiceService service;

  @BeforeEach
  void setUp() throws Exception {
    service = new SmartServiceService(repository, deactivationDetectionRepository, roleRepository, permissionRepository,
        deviceFhirClientService, TransactionOperations.withoutTransaction(), new SuspensionAdmins(ADMIN), "", "");
    when(repository.save(any(SmartService.class))).thenAnswer(returnsFirstArg());
    // behaves like the table
    when(deactivationDetectionRepository.existsById(any()))
        .thenAnswer(invocation -> handledDetections.contains(invocation.<String>getArgument(0)));
    when(deactivationDetectionRepository.save(any(DeactivationDetection.class))).thenAnswer(invocation -> {
      DeactivationDetection detection = invocation.getArgument(0);
      handledDetections.add(detection.getDetectionId());
      return detection;
    });
    serviceLog.start();
    ((Logger) LoggerFactory.getLogger(SmartServiceService.class)).addAppender(serviceLog);
  }

  @AfterEach
  void detachLog() {
    ((Logger) LoggerFactory.getLogger(SmartServiceService.class)).detachAppender(serviceLog);
  }

  private List<String> loggedMessages() {
    return serviceLog.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
  }

  private SmartService smartService(SmartServiceStatus status) {
    SmartService smartService = new SmartService();
    smartService.setId(UUID.randomUUID());
    smartService.setName("Test App");
    smartService.setClientId(CLIENT_ID);
    smartService.setFhirStoreDeviceId(CLIENT_ID);
    smartService.setStatus(status);
    return smartService;
  }

  private Device device(String clientId, FHIRDeviceStatus status) {
    Device device = new Device();
    device.setId(clientId);
    device.setStatus(status);
    Identifier identifier = new Identifier();
    identifier.setSystem(SmartServiceService.CLIENT_ID_SYSTEM);
    identifier.setValue(clientId);
    device.addIdentifier(identifier);
    return device;
  }

  @Test
  void updateStatusToRejectedMarksDeviceInactive() throws Exception {
    SmartService smartService = smartService(SmartServiceStatus.APPROVED);
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));
    when(deviceFhirClientService.getResourceByIdentifier(eq(CLIENT_ID),
        eq(SmartServiceService.CLIENT_ID_SYSTEM), isNull()))
        .thenReturn(device(CLIENT_ID, FHIRDeviceStatus.ACTIVE));

    service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.REJECTED, "tester", null);

    ArgumentCaptor<Device> stored = ArgumentCaptor.forClass(Device.class);
    verify(deviceFhirClientService).storeResource(stored.capture());
    assertThat(stored.getValue().getStatus()).isEqualTo(FHIRDeviceStatus.INACTIVE);
  }

  @Test
  void updateStatusToApprovedMarksDeviceActive() throws Exception {
    SmartService smartService = smartService(SmartServiceStatus.PENDING);
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));
    when(deviceFhirClientService.getResourceByIdentifier(eq(CLIENT_ID),
        eq(SmartServiceService.CLIENT_ID_SYSTEM), isNull()))
        .thenReturn(device(CLIENT_ID, FHIRDeviceStatus.INACTIVE));

    service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.APPROVED, "tester", null);

    ArgumentCaptor<Device> stored = ArgumentCaptor.forClass(Device.class);
    verify(deviceFhirClientService).storeResource(stored.capture());
    assertThat(stored.getValue().getStatus()).isEqualTo(FHIRDeviceStatus.ACTIVE);
  }

  @Test
  void deleteMarksDeviceInactiveBeforeRemovingService() throws Exception {
    SmartService smartService = smartService(SmartServiceStatus.APPROVED);
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));
    when(deviceFhirClientService.getResourceByIdentifier(eq(CLIENT_ID),
        eq(SmartServiceService.CLIENT_ID_SYSTEM), isNull()))
        .thenReturn(device(CLIENT_ID, FHIRDeviceStatus.ACTIVE));

    service.delete(smartService.getId(), "tester");

    ArgumentCaptor<Device> stored = ArgumentCaptor.forClass(Device.class);
    InOrder order = inOrder(deviceFhirClientService, repository);
    order.verify(deviceFhirClientService).storeResource(stored.capture());
    order.verify(repository).delete(smartService);
    assertThat(stored.getValue().getStatus()).isEqualTo(FHIRDeviceStatus.INACTIVE);
  }

  @Test
  void ensureDeviceForPendingServiceCreatesInactiveDevice() throws Exception {
    SmartService smartService = smartService(SmartServiceStatus.PENDING);
    smartService.setFhirStoreDeviceId(null);
    when(repository.findAllByFhirStoreDeviceIdIsNull()).thenReturn(Set.of(smartService));
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));
    when(deviceFhirClientService.storeResource(any(Device.class))).thenAnswer(invocation -> {
      Device stored = invocation.getArgument(0);
      stored.setId(CLIENT_ID);
      return stored;
    });
    when(deviceFhirClientService.getResources(any(ICriterion.class))).thenReturn(List.of());

    service.ensureDevices();

    ArgumentCaptor<Device> stored = ArgumentCaptor.forClass(Device.class);
    verify(deviceFhirClientService, org.mockito.Mockito.atLeastOnce()).storeResource(stored.capture());
    assertThat(stored.getAllValues().get(0).getStatus()).isEqualTo(FHIRDeviceStatus.INACTIVE);
  }

  @Test
  void reconcileMarksOrphanActiveDeviceInactive() throws Exception {
    when(repository.findAll()).thenReturn(List.of(smartService(SmartServiceStatus.APPROVED)));
    Device orphan = device(OTHER_CLIENT_ID, FHIRDeviceStatus.ACTIVE);
    Device approved = device(CLIENT_ID, FHIRDeviceStatus.ACTIVE);
    when(deviceFhirClientService.getResources()).thenReturn(List.of(orphan, approved));

    service.reconcileDeviceStatuses();

    ArgumentCaptor<Device> stored = ArgumentCaptor.forClass(Device.class);
    verify(deviceFhirClientService).storeResource(stored.capture());
    assertThat(stored.getValue().getIdElement().getIdPart()).isEqualTo(OTHER_CLIENT_ID);
    assertThat(stored.getValue().getStatus()).isEqualTo(FHIRDeviceStatus.INACTIVE);
  }

  @Test
  void reconcileRestoresApprovedServiceDeviceToActive() throws Exception {
    when(repository.findAll()).thenReturn(List.of(smartService(SmartServiceStatus.APPROVED)));
    when(deviceFhirClientService.getResources())
        .thenReturn(List.of(device(CLIENT_ID, FHIRDeviceStatus.INACTIVE)));

    service.reconcileDeviceStatuses();

    ArgumentCaptor<Device> stored = ArgumentCaptor.forClass(Device.class);
    verify(deviceFhirClientService).storeResource(stored.capture());
    assertThat(stored.getValue().getStatus()).isEqualTo(FHIRDeviceStatus.ACTIVE);
  }

  @Test
  void reconcileDeactivatesDuplicateDeviceCarryingApprovedClientId() throws Exception {
    SmartService approved = smartService(SmartServiceStatus.APPROVED);
    when(repository.findAll()).thenReturn(List.of(approved));
    Device ownDevice = device(CLIENT_ID, FHIRDeviceStatus.ACTIVE);
    Device duplicate = device(CLIENT_ID, FHIRDeviceStatus.ACTIVE);
    duplicate.setId(OTHER_CLIENT_ID);
    when(deviceFhirClientService.getResources()).thenReturn(List.of(ownDevice, duplicate));

    service.reconcileDeviceStatuses();

    ArgumentCaptor<Device> stored = ArgumentCaptor.forClass(Device.class);
    verify(deviceFhirClientService).storeResource(stored.capture());
    assertThat(stored.getValue().getIdElement().getIdPart()).isEqualTo(OTHER_CLIENT_ID);
    assertThat(stored.getValue().getStatus()).isEqualTo(FHIRDeviceStatus.INACTIVE);
  }

  @Test
  void reconcileAddsNarrativeToDevicesLackingOne() throws Exception {
    when(repository.findAll()).thenReturn(List.of());
    Device orphan = device(OTHER_CLIENT_ID, FHIRDeviceStatus.ACTIVE);
    when(deviceFhirClientService.getResources()).thenReturn(List.of(orphan));

    service.reconcileDeviceStatuses();

    ArgumentCaptor<Device> stored = ArgumentCaptor.forClass(Device.class);
    verify(deviceFhirClientService).storeResource(stored.capture());
    assertThat(stored.getValue().getText().hasDiv()).isTrue();
  }

  @Test
  void reconcileLeavesEnteredInErrorDevicesUntouched() throws Exception {
    when(repository.findAll()).thenReturn(List.of());
    when(deviceFhirClientService.getResources())
        .thenReturn(List.of(device(OTHER_CLIENT_ID, FHIRDeviceStatus.ENTEREDINERROR)));

    service.reconcileDeviceStatuses();

    verify(deviceFhirClientService, never()).storeResource(any(Device.class));
  }

  @Test
  void reconcileSkipsDevicesWithoutClientIdIdentifier() throws Exception {
    when(repository.findAll()).thenReturn(List.of());
    Device foreign = new Device();
    foreign.setId(OTHER_CLIENT_ID);
    foreign.setStatus(FHIRDeviceStatus.ACTIVE);
    Identifier other = new Identifier();
    other.setSystem("http://example.org/other-system");
    other.setValue("something");
    foreign.addIdentifier(other);
    when(deviceFhirClientService.getResources()).thenReturn(List.of(foreign));

    service.reconcileDeviceStatuses();

    verify(deviceFhirClientService, never()).storeResource(any(Device.class));
  }

  @Test
  void theMinuteJobSendsNoFhirRequestsWhenEveryServiceHasADevice() {
    when(repository.findAllByFhirStoreDeviceIdIsNull()).thenReturn(Set.of());
    when(repository.findAll()).thenReturn(List.of(smartService(SmartServiceStatus.APPROVED)));

    service.ensureDevicesJob();

    // every FHIR request becomes an AuditEvent; a search of all Devices every minute flooded the audit log
    verifyNoInteractions(deviceFhirClientService);
  }

  @Test
  void theReconciliationRunsOncePerNight() throws Exception {
    Scheduled scheduled = SmartServiceService.class.getMethod("reconcileDeviceStatuses").getAnnotation(Scheduled.class);
    assertThat(scheduled).as("reconcileDeviceStatuses is scheduled").isNotNull();
    ZoneId zone = scheduled.zone().isEmpty() ? ZoneId.systemDefault() : ZoneId.of(scheduled.zone());
    CronExpression cron = CronExpression.parse(scheduled.cron());

    ZonedDateTime first = cron.next(ZonedDateTime.of(2026, 11, 17, 12, 0, 0, 0, zone));
    ZonedDateTime second = cron.next(first);

    assertThat(Duration.between(first, second)).isEqualTo(Duration.ofDays(1));
    assertThat(first.withZoneSameInstant(ZoneId.of("Europe/Amsterdam")).getHour()).isLessThan(6);
  }

  private SmartService deactivated() {
    SmartService smartService = smartService(SmartServiceStatus.DEACTIVATED);
    smartService.setDeactivatedOn(LocalDateTime.now());
    smartService.setDeactivationReason("Rejected 300 write requests");
    return smartService;
  }

  /** Deactivated by the FHIR server first, then suspended by an admin. */
  private SmartService suspendedAfterDeactivation(String createdBy) {
    SmartService smartService = deactivated();
    smartService.setStatus(SmartServiceStatus.SUSPENDED);
    smartService.setCreatedBy(createdBy);
    return smartService;
  }

  /** Each read returns a fresh copy, as the FHIR store would. */
  private void deviceInStore(FHIRDeviceStatus status) {
    when(deviceFhirClientService.getResourceByReference(CLIENT_ID)).thenAnswer(invocation -> device(CLIENT_ID, status));
  }

  private Device storedDevice() throws Exception {
    ArgumentCaptor<Device> stored = ArgumentCaptor.forClass(Device.class);
    verify(deviceFhirClientService).storeResource(stored.capture());
    return stored.getValue();
  }

  @Test
  void deactivatesAnApprovedServiceAndItsDevice() throws Exception {
    SmartService approved = smartService(SmartServiceStatus.APPROVED);
    when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(approved));
    deviceInStore(FHIRDeviceStatus.ACTIVE);

    DeactivationResult result = service.deactivate(CLIENT_ID, DETECTION, "Rejected 300 write requests").orElseThrow();

    assertThat(result.newlyDeactivated()).isTrue();
    assertThat(result.confirmed()).isTrue();
    assertThat(approved.getStatus()).isEqualTo(SmartServiceStatus.DEACTIVATED);
    assertThat(approved.getDeactivatedOn()).isNotNull();
    assertThat(approved.getDeactivationReason()).isEqualTo("Rejected 300 write requests");
    assertThat(handledDetections).containsExactly(DETECTION);
    verify(repository).save(approved);
    assertThat(storedDevice().getStatus()).isEqualTo(FHIRDeviceStatus.INACTIVE);
  }

  @Test
  void truncatesALongReason() {
    SmartService approved = smartService(SmartServiceStatus.APPROVED);
    when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(approved));
    deviceInStore(FHIRDeviceStatus.ACTIVE);

    service.deactivate(CLIENT_ID, DETECTION, "x".repeat(1500));

    assertThat(approved.getDeactivationReason()).hasSize(SmartService.MAX_DEACTIVATION_REASON_LENGTH);
  }

  @Test
  void aFailedDeviceUpdateIsNotConfirmedAndARetryRepairsItWithoutDeactivatingAgain() throws Exception {
    SmartService approved = smartService(SmartServiceStatus.APPROVED);
    when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(approved));
    deviceInStore(FHIRDeviceStatus.ACTIVE);
    when(deviceFhirClientService.storeResource(any(Device.class)))
        .thenThrow(new IOException("FHIR server down"))
        .thenAnswer(returnsFirstArg());

    DeactivationResult first = service.deactivate(CLIENT_ID, DETECTION, "reason").orElseThrow();
    DeactivationResult retry = service.deactivate(CLIENT_ID, DETECTION, "reason").orElseThrow();

    assertThat(first.newlyDeactivated()).isTrue();
    assertThat(first.confirmed()).isFalse();
    assertThat(retry.newlyDeactivated()).isFalse();
    assertThat(retry.confirmed()).isTrue();
    verify(repository, times(1)).save(approved);
    verify(deviceFhirClientService, times(2)).storeResource(any(Device.class));
  }

  @Test
  void aMissingDeviceIsNotConfirmed() {
    when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(smartService(SmartServiceStatus.APPROVED)));
    when(deviceFhirClientService.getResourceByReference(CLIENT_ID)).thenThrow(new ResourceNotFoundException("Device/" + CLIENT_ID));

    DeactivationResult result = service.deactivate(CLIENT_ID, DETECTION, "reason").orElseThrow();

    assertThat(result.newlyDeactivated()).isTrue();
    assertThat(result.confirmed()).isFalse();
  }

  @Test
  void aRetriedDetectionLeavesAServiceApprovedAgainSinceAlone() {
    SmartService reapproved = smartService(SmartServiceStatus.APPROVED);
    handledDetections.add(DETECTION);
    when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(reapproved));

    DeactivationResult result = service.deactivate(CLIENT_ID, DETECTION, "reason").orElseThrow();

    assertThat(result.newlyDeactivated()).isFalse();
    assertThat(result.confirmed()).isTrue();
    assertThat(reapproved.getStatus()).isEqualTo(SmartServiceStatus.APPROVED);
    verify(repository, never()).save(any());
    verifyNoInteractions(deviceFhirClientService);
  }

  @Test
  void anOlderDeliveryArrivingAfterANewerDetectionChangesNothing() {
    SmartService smartService = smartService(SmartServiceStatus.APPROVED);
    when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(smartService));
    deviceInStore(FHIRDeviceStatus.ACTIVE);
    service.deactivate(CLIENT_ID, "detection-1", "reason"); // a retry of detection-1 got through first
    smartService.setStatus(SmartServiceStatus.APPROVED); // an operator approves the service again
    service.deactivate(CLIENT_ID, "detection-2", "reason"); // the client floods again
    smartService.setStatus(SmartServiceStatus.APPROVED); // and is approved again

    // the first delivery of detection-1, held up in domain-admin until now
    DeactivationResult late = service.deactivate(CLIENT_ID, "detection-1", "reason").orElseThrow();

    assertThat(late.newlyDeactivated()).isFalse();
    assertThat(late.confirmed()).isTrue();
    assertThat(smartService.getStatus()).isEqualTo(SmartServiceStatus.APPROVED);
  }

  @ParameterizedTest
  @EnumSource(value = SmartServiceStatus.class, names = {"PENDING", "REJECTED", "DEACTIVATED", "SUSPENDED"})
  void deactivatesTheDeviceOfAServiceThatIsNotApprovedAndRecordsTheDetection(SmartServiceStatus status) throws Exception {
    SmartService notApproved = smartService(status);
    when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(notApproved));
    deviceInStore(FHIRDeviceStatus.ACTIVE);

    DeactivationResult result = service.deactivate(CLIENT_ID, DETECTION, "reason").orElseThrow();

    assertThat(result.newlyDeactivated()).isFalse();
    assertThat(result.confirmed()).isTrue();
    assertThat(notApproved.getStatus()).isEqualTo(status);
    assertThat(handledDetections).containsExactly(DETECTION);
    verify(repository, never()).save(any());
    assertThat(storedDevice().getStatus()).isEqualTo(FHIRDeviceStatus.INACTIVE);
  }

  @Test
  void aRetryAfterARejectedServiceWasApprovedAgainLeavesItApproved() {
    SmartService rejected = smartService(SmartServiceStatus.REJECTED);
    when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(rejected));
    // the Device step fails, so the FHIR server will retry this detection
    when(deviceFhirClientService.getResourceByReference(CLIENT_ID)).thenThrow(new ResourceNotFoundException("Device/" + CLIENT_ID));
    assertThat(service.deactivate(CLIENT_ID, DETECTION, "reason").orElseThrow().confirmed()).isFalse();

    rejected.setStatus(SmartServiceStatus.APPROVED); // an operator approves the service before the retry arrives
    DeactivationResult retry = service.deactivate(CLIENT_ID, DETECTION, "reason").orElseThrow();

    assertThat(retry.newlyDeactivated()).isFalse();
    assertThat(retry.confirmed()).isTrue();
    assertThat(rejected.getStatus()).isEqualTo(SmartServiceStatus.APPROVED);
  }

  @Test
  void confirmsAnAlreadyInactiveDeviceWithoutStoringIt() throws Exception {
    handledDetections.add(DETECTION); // a retry
    when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(smartService(SmartServiceStatus.REJECTED)));
    deviceInStore(FHIRDeviceStatus.INACTIVE);

    assertThat(service.deactivate(CLIENT_ID, DETECTION, "reason").orElseThrow().confirmed()).isTrue();
    verify(deviceFhirClientService, never()).storeResource(any());
  }

  @Test
  void anUnknownClientIsNotDeactivated() {
    when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

    assertThat(service.deactivate(CLIENT_ID, DETECTION, "reason")).isEmpty();
    assertThat(handledDetections).isEmpty();
  }

  @Test
  void onlyASuspensionAdminCanChangeTheStatusOfASuspendedService() throws Exception {
    SmartService smartService = suspendedAfterDeactivation("creator-sub");
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));

    assertThatThrownBy(() -> service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.APPROVED,
        "creator-sub", "vendor@example.com")).isInstanceOf(SmartServiceRegistrationException.class);
    assertThatThrownBy(() -> service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.REJECTED,
        "other-sub", null)).isInstanceOf(SmartServiceRegistrationException.class);

    assertThat(smartService.getStatus()).isEqualTo(SmartServiceStatus.SUSPENDED);
    verify(repository, never()).save(any());
    verify(deviceFhirClientService, never()).storeResource(any());
  }

  @Test
  void aSuspensionAdminLiftsTheSuspension() throws Exception {
    // the admin rule replaces the creator rule, so an admin may lift it also for an application they registered
    SmartService smartService = suspendedAfterDeactivation("admin-sub");
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));
    when(deviceFhirClientService.getResourceByIdentifier(eq(CLIENT_ID),
        eq(SmartServiceService.CLIENT_ID_SYSTEM), isNull()))
        .thenReturn(device(CLIENT_ID, FHIRDeviceStatus.INACTIVE));

    SmartService result = service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.APPROVED, "admin-sub", ADMIN);

    assertThat(result.getStatus()).isEqualTo(SmartServiceStatus.APPROVED);
    assertThat(result.getDeactivatedOn()).isNull();
    assertThat(result.getDeactivationReason()).isNull();
    assertThat(storedDevice().getStatus()).isEqualTo(FHIRDeviceStatus.ACTIVE);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {ADMIN, "vendor@example.com"})
  void nobodyDeactivatesByHand(String userEmail) {
    SmartService smartService = smartService(SmartServiceStatus.APPROVED);
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));

    assertThatThrownBy(() -> service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.DEACTIVATED,
        "user-sub", userEmail)).isInstanceOf(SmartServiceRegistrationException.class);

    assertThat(smartService.getStatus()).isEqualTo(SmartServiceStatus.APPROVED);
    verify(repository, never()).save(any());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"vendor@example.com"})
  void onlyASuspensionAdminCanSuspend(String userEmail) {
    SmartService smartService = smartService(SmartServiceStatus.APPROVED);
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));

    assertThatThrownBy(() -> service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.SUSPENDED,
        "vendor-sub", userEmail)).isInstanceOf(SmartServiceRegistrationException.class);

    assertThat(smartService.getStatus()).isEqualTo(SmartServiceStatus.APPROVED);
    verify(repository, never()).save(any());
  }

  @Test
  void aSuspensionAdminSuspendsAnApprovedServiceByHand() throws Exception {
    SmartService smartService = smartService(SmartServiceStatus.APPROVED);
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));
    when(deviceFhirClientService.getResourceByIdentifier(eq(CLIENT_ID),
        eq(SmartServiceService.CLIENT_ID_SYSTEM), isNull()))
        .thenReturn(device(CLIENT_ID, FHIRDeviceStatus.ACTIVE));

    SmartService result = service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.SUSPENDED, "admin-sub", ADMIN);

    assertThat(result.getStatus()).isEqualTo(SmartServiceStatus.SUSPENDED);
    assertThat(storedDevice().getStatus()).isEqualTo(FHIRDeviceStatus.INACTIVE);
  }

  @Test
  void suspendingADeactivatedServiceKeepsWhyItWasDeactivated() {
    SmartService smartService = deactivated();
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));

    SmartService result = service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.SUSPENDED, "admin-sub", ADMIN);

    assertThat(result.getStatus()).isEqualTo(SmartServiceStatus.SUSPENDED);
    assertThat(result.getDeactivatedOn()).isNotNull();
    assertThat(result.getDeactivationReason()).isEqualTo("Rejected 300 write requests");
  }

  @Test
  void anyoneCanReactivateADeactivatedService() throws Exception {
    SmartService smartService = deactivated();
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));
    when(deviceFhirClientService.getResourceByIdentifier(eq(CLIENT_ID),
        eq(SmartServiceService.CLIENT_ID_SYSTEM), isNull()))
        .thenReturn(device(CLIENT_ID, FHIRDeviceStatus.INACTIVE));

    SmartService result = service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.APPROVED,
        "vendor-sub", "vendor@example.com");

    assertThat(result.getStatus()).isEqualTo(SmartServiceStatus.APPROVED);
    assertThat(result.getDeactivatedOn()).isNull();
    assertThat(result.getDeactivationReason()).isNull();
    assertThat(storedDevice().getStatus()).isEqualTo(FHIRDeviceStatus.ACTIVE);
  }

  @Test
  void logsWhoSuspendedWhichClient() {
    // the Device of a deactivated service is inactive already, so no Device update names the client
    SmartService smartService = deactivated();
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));

    service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.SUSPENDED, "admin-sub", ADMIN);

    assertThat(loggedMessages()).contains("User [admin-sub] changed the status of SmartService with client_id ["
        + CLIENT_ID + "] from [DEACTIVATED] to [SUSPENDED]");
  }

  @Test
  void logsNoStatusChangeThatWasNotSaved() {
    SmartService smartService = smartService(SmartServiceStatus.APPROVED);
    when(repository.findById(smartService.getId())).thenReturn(Optional.of(smartService));
    doThrow(new OptimisticLockingFailureException("changed meanwhile")).when(repository).save(any(SmartService.class));

    assertThatThrownBy(() -> service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.REJECTED,
        "tester", null)).isInstanceOf(OptimisticLockingFailureException.class);

    assertThat(loggedMessages()).noneMatch(message -> message.startsWith("User [tester] changed"));
  }

  @Test
  void aReconciliationStartedBeforeADeactivationCannotReactivateTheDevice() throws Exception {
    Device device = device(CLIENT_ID, FHIRDeviceStatus.ACTIVE); // the single Device in the store
    when(deviceFhirClientService.getResourceByReference(CLIENT_ID)).thenReturn(device);
    // the reconciliation's snapshot is a separate copy that still says approved
    when(repository.findAll()).thenReturn(List.of(smartService(SmartServiceStatus.APPROVED)));
    when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(smartService(SmartServiceStatus.APPROVED)));
    CountDownLatch fetchingDevices = new CountDownLatch(1);
    CountDownLatch devicesFetched = new CountDownLatch(1);
    when(deviceFhirClientService.getResources()).thenAnswer(invocation -> {
      fetchingDevices.countDown();
      devicesFetched.await(5, TimeUnit.SECONDS);
      return List.of(device);
    });

    ExecutorService threads = Executors.newFixedThreadPool(2);
    try {
      Future<?> reconciliation = threads.submit(service::reconcileDeviceStatuses);
      assertThat(fetchingDevices.await(5, TimeUnit.SECONDS)).isTrue();
      Future<?> deactivation = threads.submit(() -> service.deactivate(CLIENT_ID, DETECTION, "reason"));
      try {
        deactivation.get(500, TimeUnit.MILLISECONDS); // without coordination the deactivation completes here
      } catch (TimeoutException waitingForTheReconciliation) {
        // expected with the lock
      }
      devicesFetched.countDown();
      reconciliation.get(5, TimeUnit.SECONDS);
      deactivation.get(5, TimeUnit.SECONDS);
    } finally {
      devicesFetched.countDown();
      threads.shutdownNow();
    }

    assertThat(device.getStatus()).isEqualTo(FHIRDeviceStatus.INACTIVE);
  }
}
