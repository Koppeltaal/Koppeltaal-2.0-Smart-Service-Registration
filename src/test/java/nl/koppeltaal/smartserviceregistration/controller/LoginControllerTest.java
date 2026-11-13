package nl.koppeltaal.smartserviceregistration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auth0.jwk.JwkException;
import nl.koppeltaal.smartserviceregistration.oidc.service.OidcClientService;
import nl.koppeltaal.smartserviceregistration.service.DomainAdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

  @Mock private OidcClientService oidcClientService;
  @Mock private DomainAdminUserService domainAdminUserService;

  private LoginController controller;
  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    controller = new LoginController(oidcClientService, domainAdminUserService);
    session = new MockHttpSession();
    session.setAttribute("state", "state-1");
    when(oidcClientService.getUserIdFromCredentials(any())).thenReturn("sub-1");
    when(oidcClientService.getUserIdentifierFromCredentials(any())).thenReturn("vendor@example.com");
  }

  @Test
  void recordsTheLoginWithTheEmail() throws Exception {
    String view = controller.codeResponse(session, new MockHttpServletRequest(), "code-1", "state-1");

    assertThat(view).isEqualTo("redirect:/");
    assertThat(session.getAttribute("user")).isEqualTo("sub-1");
    // the login email decides who is a suspension admin
    assertThat(session.getAttribute("email")).isEqualTo("vendor@example.com");
    verify(domainAdminUserService).recordLogin("sub-1", "vendor@example.com");
  }

  @Test
  void keepsNoEmailInTheSessionWhenTheTokenHasNone() throws Exception {
    // without an email claim the identifier is the sub
    when(oidcClientService.getUserIdentifierFromCredentials(any())).thenReturn("sub-1");

    controller.codeResponse(session, new MockHttpServletRequest(), "code-1", "state-1");

    assertThat(session.getAttribute("email")).isNull();
    verify(domainAdminUserService).recordLogin("sub-1", "sub-1");
  }

  @Test
  void aLoginWithoutAnEmailClaimDropsTheEmailOfAnEarlierLoginInTheSameSession() throws Exception {
    session.setAttribute("email", "joris@headease.nl"); // an admin logged in earlier in this session
    when(oidcClientService.getUserIdentifierFromCredentials(any())).thenReturn("sub-1");

    controller.codeResponse(session, new MockHttpServletRequest(), "code-1", "state-1");

    assertThat(session.getAttribute("user")).isEqualTo("sub-1");
    assertThat(session.getAttribute("email")).isNull();
  }

  @Test
  void aFailingClaimLookupDropsTheEmailOfAnEarlierLoginInTheSameSession() throws Exception {
    session.setAttribute("email", "joris@headease.nl"); // an admin logged in earlier in this session
    when(oidcClientService.getUserIdentifierFromCredentials(any())).thenThrow(new JwkException("unknown key"));

    String view = controller.codeResponse(session, new MockHttpServletRequest(), "code-1", "state-1");

    assertThat(view).isEqualTo("redirect:/");
    assertThat(session.getAttribute("user")).isEqualTo("sub-1");
    assertThat(session.getAttribute("email")).isNull();
  }

  @Test
  void aFailureToRecordDoesNotBlockTheLogin() throws Exception {
    doThrow(new IllegalStateException("database down")).when(domainAdminUserService).recordLogin(any(), any());

    String view = controller.codeResponse(session, new MockHttpServletRequest(), "code-1", "state-1");

    assertThat(view).isEqualTo("redirect:/");
    assertThat(session.getAttribute("user")).isEqualTo("sub-1");
    assertThat(session.getAttribute("email")).isEqualTo("vendor@example.com");
  }

  @Test
  void keepsASubThatIsAnEmailAddressAsTheEmail() throws Exception {
    // in a realm that uses the email address as the subject, the email claim equals the sub
    when(oidcClientService.getUserIdFromCredentials(any())).thenReturn("joris@headease.nl");
    when(oidcClientService.getUserIdentifierFromCredentials(any())).thenReturn("joris@headease.nl");

    controller.codeResponse(session, new MockHttpServletRequest(), "code-1", "state-1");

    assertThat(session.getAttribute("email")).isEqualTo("joris@headease.nl");
    verify(domainAdminUserService).recordLogin("joris@headease.nl", "joris@headease.nl");
  }
}
