package nl.koppeltaal.smartserviceregistration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.gclient.ICriterion;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.repository.PermissionRepository;
import nl.koppeltaal.smartserviceregistration.repository.RoleRepository;
import nl.koppeltaal.smartserviceregistration.repository.SmartServiceRepository;
import nl.koppeltaal.spring.boot.starter.smartservice.service.fhir.DeviceFhirClientService;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Device.FHIRDeviceStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SmartServiceServiceTest {

  private static final String CLIENT_ID = "11111111-2222-3333-4444-555555555555";
  private static final String OTHER_CLIENT_ID = "99999999-8888-7777-6666-555555555555";

  @Mock private SmartServiceRepository repository;
  @Mock private RoleRepository roleRepository;
  @Mock private PermissionRepository permissionRepository;
  @Mock private DeviceFhirClientService deviceFhirClientService;

  private SmartServiceService service;

  @BeforeEach
  void setUp() throws Exception {
    service = new SmartServiceService(repository, roleRepository, permissionRepository,
        deviceFhirClientService, "", "");
    when(repository.save(any(SmartService.class))).thenAnswer(returnsFirstArg());
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

    service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.REJECTED, "tester");

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

    service.updateSmartServiceStatus(smartService.getId(), SmartServiceStatus.APPROVED, "tester");

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
}
