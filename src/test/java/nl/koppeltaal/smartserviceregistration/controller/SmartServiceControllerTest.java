package nl.koppeltaal.smartserviceregistration.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import java.util.UUID;
import nl.koppeltaal.smartserviceregistration.config.SmartServiceConfiguration;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.repository.IdentityProviderRepository;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SmartServiceController.class)
class SmartServiceControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private SmartServiceService smartServiceService;
  @MockBean private SmartServiceConfiguration smartServiceConfiguration;
  @MockBean private IdentityProviderRepository identityProviderRepository;

  @Test
  void aConcurrentChangeSendsTheUserBackToTheOverview() throws Exception {
    UUID id = UUID.randomUUID();
    when(smartServiceService.updateSmartServiceStatus(id, SmartServiceStatus.APPROVED, "beheer-sub", null))
        .thenThrow(new ObjectOptimisticLockingFailureException(SmartService.class, id));

    mockMvc.perform(post("/smart-service/status")
            .sessionAttr("user", "beheer-sub")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("id", id.toString())
            .param("status", "APPROVED"))
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void passesTheLoginEmailWithAStatusChange() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(post("/smart-service/status")
            .sessionAttr("user", "admin-sub")
            .sessionAttr("email", "joris@headease.nl")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("id", id.toString())
            .param("status", "APPROVED"))
        .andExpect(redirectedUrl("/"));

    verify(smartServiceService).updateSmartServiceStatus(id, SmartServiceStatus.APPROVED, "admin-sub", "joris@headease.nl");
  }
}
