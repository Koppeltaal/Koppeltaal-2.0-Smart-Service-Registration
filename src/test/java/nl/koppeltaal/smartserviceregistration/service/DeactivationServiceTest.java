package nl.koppeltaal.smartserviceregistration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.model.DeactivationMode;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService.DeactivationResult;
import nl.koppeltaal.smartserviceregistration.service.DeactivationService.Outcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class DeactivationServiceTest {

  private static final String CLIENT_ID = "11111111-2222-3333-4444-555555555555";
  private static final String DETECTION = "6c3b9b8e-1d1f-4c55-9d43-1f0c2f3a9b10";
  private static final String REASON = "Rejected 300 write requests";

  @Mock private SmartServiceService smartServiceService;
  @Mock private DeactivationMailer mailer;
  @InjectMocks private DeactivationService service;

  private static SmartService smartService(SmartServiceStatus status) {
    SmartService smartService = new SmartService();
    smartService.setClientId(CLIENT_ID);
    smartService.setStatus(status);
    return smartService;
  }

  private void deactivationResult(SmartService smartService, boolean newlyDeactivated, boolean confirmed) {
    when(smartServiceService.deactivate(CLIENT_ID, DETECTION, REASON))
        .thenReturn(Optional.of(new DeactivationResult(smartService, newlyDeactivated, confirmed)));
  }

  @Test
  void confirmsANewDeactivationAndMailsIt() {
    SmartService deactivated = smartService(SmartServiceStatus.DEACTIVATED);
    deactivationResult(deactivated, true, true);

    assertThat(service.handle(CLIENT_ID, DeactivationMode.ENFORCE, DETECTION, REASON)).isEqualTo(Outcome.CONFIRMED);

    verify(mailer).sendDeactivation(deactivated);
  }

  @Test
  void mailsANewDeactivationButAsksForARetryWhileTheDeviceIsActive() {
    SmartService deactivated = smartService(SmartServiceStatus.DEACTIVATED);
    deactivationResult(deactivated, true, false);

    assertThat(service.handle(CLIENT_ID, DeactivationMode.ENFORCE, DETECTION, REASON)).isEqualTo(Outcome.RETRY);

    verify(mailer).sendDeactivation(deactivated);
  }

  @Test
  void confirmsWithoutMailWhenNothingWasDeactivatedNow() {
    deactivationResult(smartService(SmartServiceStatus.DEACTIVATED), false, true);

    assertThat(service.handle(CLIENT_ID, DeactivationMode.ENFORCE, DETECTION, REASON)).isEqualTo(Outcome.CONFIRMED);

    verifyNoInteractions(mailer);
  }

  @Test
  void asksForARetryWhenTheServiceChangedMeanwhile() {
    when(smartServiceService.deactivate(CLIENT_ID, DETECTION, REASON))
        .thenThrow(new ObjectOptimisticLockingFailureException(SmartService.class, CLIENT_ID));

    assertThat(service.handle(CLIENT_ID, DeactivationMode.ENFORCE, DETECTION, REASON)).isEqualTo(Outcome.RETRY);

    verifyNoInteractions(mailer);
  }

  @Test
  void aReportOnlyMails() {
    SmartService approved = smartService(SmartServiceStatus.APPROVED);
    when(smartServiceService.findByClientId(CLIENT_ID)).thenReturn(Optional.of(approved));

    assertThat(service.handle(CLIENT_ID, DeactivationMode.REPORT, DETECTION, REASON)).isEqualTo(Outcome.REPORTED);

    verify(mailer).sendReport(approved, REASON);
    verify(smartServiceService, never()).deactivate(any(), any(), any());
  }

  @Test
  void anUnknownClientChangesNothing() {
    when(smartServiceService.deactivate(CLIENT_ID, DETECTION, REASON)).thenReturn(Optional.empty());
    when(smartServiceService.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

    assertThat(service.handle(CLIENT_ID, DeactivationMode.ENFORCE, DETECTION, REASON)).isEqualTo(Outcome.UNKNOWN_CLIENT);
    assertThat(service.handle(CLIENT_ID, DeactivationMode.REPORT, DETECTION, REASON)).isEqualTo(Outcome.UNKNOWN_CLIENT);

    verifyNoInteractions(mailer);
  }
}
