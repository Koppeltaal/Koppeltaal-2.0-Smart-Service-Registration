package nl.koppeltaal.smartserviceregistration.controller;

import nl.koppeltaal.smartserviceregistration.dto.DeactivationRequestDto;
import nl.koppeltaal.smartserviceregistration.model.DeactivationDetection;
import nl.koppeltaal.smartserviceregistration.service.DeactivationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by the FHIR server. The LoggedInInterceptor lets a call through only with the X-Deactivation-Token secret.
 */
@RestController
public class DeactivationController {

  private final DeactivationService deactivationService;

  public DeactivationController(DeactivationService deactivationService) {
    this.deactivationService = deactivationService;
  }

  @PostMapping(value = "/deactivations", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> report(@RequestBody DeactivationRequestDto request) {
    if (StringUtils.isBlank(request.getClientId()) || request.getMode() == null
        || StringUtils.isBlank(request.getDetectionId())
        || request.getDetectionId().length() > DeactivationDetection.MAX_ID_LENGTH) {
      return ResponseEntity.badRequest().build();
    }
    return switch (deactivationService.handle(request.getClientId(), request.getMode(), request.getDetectionId(), request.getReason())) {
      case CONFIRMED, REPORTED -> ResponseEntity.noContent().build();
      case RETRY -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      case UNKNOWN_CLIENT -> ResponseEntity.notFound().build();
    };
  }
}
