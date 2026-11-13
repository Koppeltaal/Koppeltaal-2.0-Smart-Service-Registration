package nl.koppeltaal.smartserviceregistration.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import nl.koppeltaal.smartserviceregistration.model.DeactivationMode;
import nl.koppeltaal.smartserviceregistration.service.DeactivationService;
import nl.koppeltaal.smartserviceregistration.service.DeactivationService.Outcome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = DeactivationController.class, properties = "deactivation.endpoint.token=deactivation-secret")
class DeactivationControllerTest {

  private static final String DETECTION = "6c3b9b8e-1d1f-4c55-9d43-1f0c2f3a9b10";

  @Autowired private MockMvc mockMvc;
  @MockBean private DeactivationService deactivationService;

  /** The LoggedInInterceptor branches on the servlet path, which MockMvc leaves empty by default. */
  private static MockHttpServletRequestBuilder deactivations(String body) {
    return post("/deactivations").servletPath("/deactivations").contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private ResultActions postWithSecret(String body) throws Exception {
    return mockMvc.perform(deactivations(body).header("X-Deactivation-Token", "deactivation-secret"));
  }

  private static String body(String mode) {
    return "{\"clientId\":\"client-1\",\"mode\":\"" + mode + "\",\"detectionId\":\"" + DETECTION + "\",\"reason\":\"reason\"}";
  }

  @Test
  void answersNoContentWhenConfirmedOrReported() throws Exception {
    when(deactivationService.handle("client-1", DeactivationMode.ENFORCE, DETECTION, "reason")).thenReturn(Outcome.CONFIRMED);
    when(deactivationService.handle("client-1", DeactivationMode.REPORT, DETECTION, "reason")).thenReturn(Outcome.REPORTED);

    postWithSecret(body("ENFORCE")).andExpect(status().isNoContent());
    postWithSecret(body("REPORT")).andExpect(status().isNoContent());
  }

  @Test
  void answersServiceUnavailableUntilConfirmed() throws Exception {
    when(deactivationService.handle("client-1", DeactivationMode.ENFORCE, DETECTION, "reason")).thenReturn(Outcome.RETRY);

    postWithSecret(body("ENFORCE")).andExpect(status().isServiceUnavailable());
  }

  @Test
  void answersNotFoundForAnUnknownClient() throws Exception {
    when(deactivationService.handle("client-1", DeactivationMode.ENFORCE, DETECTION, "reason")).thenReturn(Outcome.UNKNOWN_CLIENT);

    postWithSecret(body("ENFORCE")).andExpect(status().isNotFound());
  }

  @Test
  void answersBadRequestForMissingOrInvalidFields() throws Exception {
    postWithSecret("{\"mode\":\"ENFORCE\",\"detectionId\":\"" + DETECTION + "\"}").andExpect(status().isBadRequest());
    postWithSecret("{\"clientId\":\" \",\"mode\":\"ENFORCE\",\"detectionId\":\"" + DETECTION + "\"}").andExpect(status().isBadRequest());
    postWithSecret("{\"clientId\":\"client-1\",\"detectionId\":\"" + DETECTION + "\"}").andExpect(status().isBadRequest());
    postWithSecret("{\"clientId\":\"client-1\",\"mode\":\"PAUSE\",\"detectionId\":\"" + DETECTION + "\"}").andExpect(status().isBadRequest());
    postWithSecret("{\"clientId\":\"client-1\",\"mode\":\"ENFORCE\"}").andExpect(status().isBadRequest());
    postWithSecret("{\"clientId\":\"client-1\",\"mode\":\"ENFORCE\",\"detectionId\":\"" + "x".repeat(65) + "\"}")
        .andExpect(status().isBadRequest());

    verifyNoInteractions(deactivationService);
  }

  @Test
  void refusesACallWithoutTheSecret() throws Exception {
    mockMvc.perform(deactivations(body("ENFORCE"))).andExpect(status().isUnauthorized());

    verifyNoInteractions(deactivationService);
  }
}
