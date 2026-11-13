package nl.koppeltaal.smartserviceregistration.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.service.RoleService;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService;
import nl.koppeltaal.smartserviceregistration.service.SuspensionAdmins;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

  private static final String LOCKED_SELECT = "<select name=\"status\" onchange=\"this.form.submit()\" disabled=\"disabled\">";
  private static final String DEACTIVATION = "Automatisch gedeactiveerd op 11-11-2026 10:04: Rejected 300 write requests";
  private static final String REACTIVATION_HINT = "Herstel de fout en zet de status daarna weer op Approved.";
  private static final String SUSPENSION_NOTE = "Geschorst door Koppeltaal beheer. Alleen beheer kan de schorsing opheffen.";
  private static final String ADMIN = "joris@headease.nl";

  @Autowired private MockMvc mockMvc;
  @MockBean private SmartServiceService smartServiceService;
  @MockBean private RoleService roleService;
  @MockBean private SuspensionAdmins suspensionAdmins;

  private static SmartService smartService(SmartServiceStatus status) {
    SmartService smartService = new SmartService();
    smartService.setId(UUID.randomUUID());
    smartService.setName("Test App");
    smartService.setStatus(status);
    return smartService;
  }

  /** Deactivated by the FHIR server; a suspension by an admin afterwards keeps the time and reason. */
  private static SmartService deactivatedFirst(SmartServiceStatus status) {
    SmartService smartService = smartService(status);
    smartService.setDeactivatedOn(LocalDateTime.of(2026, 11, 11, 10, 4));
    smartService.setDeactivationReason("Rejected 300 write requests");
    return smartService;
  }

  private ResultActions show(SmartService smartService, String email) throws Exception {
    when(smartServiceService.findAll()).thenReturn(List.of(smartService));
    when(roleService.findAll()).thenReturn(List.of());
    when(suspensionAdmins.isAdmin(ADMIN)).thenReturn(true);
    return mockMvc.perform(get("/").sessionAttr("user", "some-sub").sessionAttr("email", email))
        .andExpect(status().isOk());
  }

  @Test
  void showsADeactivatedServiceWithItsTimeAndReasonAndLetsAnyoneReactivateIt() throws Exception {
    show(deactivatedFirst(SmartServiceStatus.DEACTIVATED), "vendor@example.com")
        // selected so the select shows it, disabled so nobody can choose it by hand
        .andExpect(content().string(containsString("<option value=\"DEACTIVATED\" selected disabled>⏸ Deactivated</option>")))
        .andExpect(content().string(containsString(DEACTIVATION)))
        .andExpect(content().string(containsString(REACTIVATION_HINT)))
        .andExpect(content().string(not(containsString(LOCKED_SELECT))))
        .andExpect(content().string(not(containsString("Suspended"))));
  }

  @Test
  void showsASuspendedServiceLockedForOthers() throws Exception {
    show(deactivatedFirst(SmartServiceStatus.SUSPENDED), "vendor@example.com")
        .andExpect(content().string(containsString("<option value=\"SUSPENDED\" selected disabled>🔒 Suspended</option>")))
        .andExpect(content().string(containsString(LOCKED_SELECT)))
        .andExpect(content().string(containsString(SUSPENSION_NOTE)))
        .andExpect(content().string(containsString(DEACTIVATION)))
        .andExpect(content().string(not(containsString(REACTIVATION_HINT))));
  }

  @Test
  void letsASuspensionAdminLiftASuspension() throws Exception {
    show(deactivatedFirst(SmartServiceStatus.SUSPENDED), ADMIN)
        .andExpect(content().string(containsString("<option value=\"SUSPENDED\" selected=\"selected\">🔒 Suspended</option>")))
        .andExpect(content().string(containsString(SUSPENSION_NOTE)))
        .andExpect(content().string(not(containsString(LOCKED_SELECT))));
  }

  @Test
  void offersASuspensionAdminTheSuspendedOption() throws Exception {
    show(smartService(SmartServiceStatus.APPROVED), ADMIN)
        .andExpect(content().string(containsString("<option value=\"SUSPENDED\">🔒 Suspended</option>")))
        .andExpect(content().string(not(containsString("Deactivated"))))
        .andExpect(content().string(not(containsString(LOCKED_SELECT))));
  }

  @Test
  void offersOthersNeitherSuspendedNorDeactivated() throws Exception {
    show(smartService(SmartServiceStatus.APPROVED), "vendor@example.com")
        .andExpect(content().string(not(containsString("Suspended"))))
        .andExpect(content().string(not(containsString("Deactivated"))))
        .andExpect(content().string(not(containsString("gedeactiveerd op"))))
        .andExpect(content().string(not(containsString(LOCKED_SELECT))));
  }
}
