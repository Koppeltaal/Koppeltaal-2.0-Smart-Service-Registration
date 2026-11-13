package nl.koppeltaal.smartserviceregistration.service;

import java.util.Optional;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.DeactivationMode;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService.DeactivationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Handles the FHIR server's report of a client that keeps resending rejected requests.
 */
@Service
public class DeactivationService {

  private static final Logger LOG = LoggerFactory.getLogger(DeactivationService.class);

  public enum Outcome {
    /** The FHIR server may lift its own block. */
    CONFIRMED,
    /** Not confirmed yet; the FHIR server keeps its block and retries the same detection. */
    RETRY,
    REPORTED,
    UNKNOWN_CLIENT
  }

  private final SmartServiceService smartServiceService;
  private final DeactivationMailer mailer;

  public DeactivationService(SmartServiceService smartServiceService, DeactivationMailer mailer) {
    this.smartServiceService = smartServiceService;
    this.mailer = mailer;
  }

  public Outcome handle(String clientId, DeactivationMode mode, String detectionId, String reason) {
    if (mode == DeactivationMode.REPORT) {
      final Optional<SmartService> found = smartServiceService.findByClientId(clientId);
      if (found.isEmpty()) {
        LOG.warn("The FHIR server reported unknown client_id [{}]", clientId);
        return Outcome.UNKNOWN_CLIENT;
      }
      LOG.warn("The FHIR server would deactivate client_id [{}]: {}", clientId, reason);
      mailer.sendReport(found.get(), reason);
      return Outcome.REPORTED;
    }

    final Optional<DeactivationResult> result;
    try {
      result = smartServiceService.deactivate(clientId, detectionId, reason);
    } catch (OptimisticLockingFailureException e) {
      LOG.warn("Client_id [{}] changed while deactivating it; the FHIR server will retry", clientId);
      return Outcome.RETRY;
    }
    if (result.isEmpty()) {
      LOG.warn("The FHIR server reported unknown client_id [{}]", clientId);
      return Outcome.UNKNOWN_CLIENT;
    }
    if (result.get().newlyDeactivated()) {
      mailer.sendDeactivation(result.get().smartService());
    }
    return result.get().confirmed() ? Outcome.CONFIRMED : Outcome.RETRY;
  }
}
