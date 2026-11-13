package nl.koppeltaal.smartserviceregistration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class DeactivationMailerTest {

  private static final String ADMINS = "joris@headease.nl,roland@headease.nl";

  @Mock private JavaMailSender sender;
  @Mock private ObjectProvider<JavaMailSender> senderProvider;
  @Mock private DomainAdminUserService domainAdminUserService;

  static SmartService deactivated() {
    SmartService smartService = new SmartService();
    smartService.setName("Test App");
    smartService.setClientId("client-1");
    smartService.setCreatedBy("creator-sub");
    smartService.setStatus(SmartServiceStatus.DEACTIVATED);
    smartService.setDeactivationReason("Rejected 300 write requests");
    return smartService;
  }

  private DeactivationMailer mailer(String admins) {
    return new DeactivationMailer(senderProvider, domainAdminUserService, new SuspensionAdmins(admins),
        "noreply@koppeltaal.nl", "info@headease.nl");
  }

  private SimpleMailMessage sentMessage() {
    ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender).send(message.capture());
    return message.getValue();
  }

  @Test
  void mailsADeactivationToTheSuspensionAdminsAndTheCreator() {
    when(senderProvider.getIfAvailable()).thenReturn(sender);
    when(domainAdminUserService.findEmail("creator-sub")).thenReturn(Optional.of("vendor@example.com"));

    mailer(ADMINS).sendDeactivation(deactivated());

    SimpleMailMessage message = sentMessage();
    assertThat(message.getTo()).containsExactly("joris@headease.nl", "roland@headease.nl", "vendor@example.com");
    assertThat(message.getFrom()).isEqualTo("noreply@koppeltaal.nl");
    assertThat(message.getReplyTo()).isEqualTo("info@headease.nl");
    assertThat(message.getSubject()).isEqualTo("Koppeltaal: applicatie Test App is automatisch gedeactiveerd");
    assertThat(message.getText()).contains(
        "\"Test App\" (client_id client-1) automatisch gedeactiveerd, omdat de client een fout bevat",
        "Reden: Rejected 300 write requests",
        "TOP-KT-012b",
        "Zet de applicatie daarna in domain-admin weer op Approved.",
        "dan kan Koppeltaal beheer haar schorsen. Een schorsing kan alleen beheer opheffen.",
        "Neem contact op met Koppeltaal beheer via info@headease.nl of beantwoord deze e-mail.");
  }

  @Test
  void mailsACreatorWhoIsAnAdminOnce() {
    when(senderProvider.getIfAvailable()).thenReturn(sender);
    when(domainAdminUserService.findEmail("creator-sub")).thenReturn(Optional.of("Joris@Headease.nl"));

    mailer(ADMINS).sendDeactivation(deactivated());

    assertThat(sentMessage().getTo()).containsExactly("joris@headease.nl", "roland@headease.nl");
  }

  @Test
  void mailsADeactivationOnlyToTheAdminsWhenTheCreatorsEmailIsUnknown() {
    when(senderProvider.getIfAvailable()).thenReturn(sender);
    when(domainAdminUserService.findEmail("creator-sub")).thenReturn(Optional.empty());

    mailer(ADMINS).sendDeactivation(deactivated());

    assertThat(sentMessage().getTo()).containsExactly("joris@headease.nl", "roland@headease.nl");
  }

  @Test
  void mailsADeactivationOnlyToTheCreatorWithoutAdmins() {
    when(senderProvider.getIfAvailable()).thenReturn(sender);
    when(domainAdminUserService.findEmail("creator-sub")).thenReturn(Optional.of("vendor@example.com"));

    mailer("").sendDeactivation(deactivated());

    assertThat(sentMessage().getTo()).containsExactly("vendor@example.com");
  }

  @Test
  void mailsAReportOnlyToTheAdmins() {
    when(senderProvider.getIfAvailable()).thenReturn(sender);

    mailer(ADMINS).sendReport(deactivated(), "Rejected 300 write requests");

    SimpleMailMessage message = sentMessage();
    assertThat(message.getTo()).containsExactly("joris@headease.nl", "roland@headease.nl");
    assertThat(message.getReplyTo()).isEqualTo("info@headease.nl");
    assertThat(message.getSubject()).isEqualTo("Koppeltaal: applicatie Test App zou automatisch gedeactiveerd worden");
    assertThat(message.getText()).contains("client-1", "rapportagemodus. Er is niets geblokkeerd.",
        "Reden: Rejected 300 write requests");
    verifyNoInteractions(domainAdminUserService);
  }

  @Test
  void namesNoContactAddressWithoutAReplyTo() {
    when(senderProvider.getIfAvailable()).thenReturn(sender);
    when(domainAdminUserService.findEmail("creator-sub")).thenReturn(Optional.empty());

    new DeactivationMailer(senderProvider, domainAdminUserService, new SuspensionAdmins(ADMINS), "noreply@koppeltaal.nl", "")
        .sendDeactivation(deactivated());

    SimpleMailMessage message = sentMessage();
    assertThat(message.getReplyTo()).isNull();
    assertThat(message.getText()).endsWith("Vragen? Neem contact op met Koppeltaal beheer.");
  }

  @Test
  void sendsNothingWithoutAMailServer() {
    when(senderProvider.getIfAvailable()).thenReturn(null);
    when(domainAdminUserService.findEmail("creator-sub")).thenReturn(Optional.of("vendor@example.com"));

    assertThatCode(() -> mailer(ADMINS).sendDeactivation(deactivated())).doesNotThrowAnyException();

    verifyNoInteractions(sender);
  }

  @Test
  void sendsNothingWithoutRecipients() {
    when(senderProvider.getIfAvailable()).thenReturn(sender);
    when(domainAdminUserService.findEmail("creator-sub")).thenReturn(Optional.empty());

    mailer("").sendDeactivation(deactivated());

    verifyNoInteractions(sender);
  }

  @Test
  void aSendingFailureIsLoggedNotThrown() {
    when(senderProvider.getIfAvailable()).thenReturn(sender);
    when(domainAdminUserService.findEmail("creator-sub")).thenReturn(Optional.empty());
    doThrow(new MailSendException("smtp down")).when(sender).send(any(SimpleMailMessage.class));

    assertThatCode(() -> mailer(ADMINS).sendDeactivation(deactivated())).doesNotThrowAnyException();
  }

  @Test
  void aFailingCreatorLookupStillMailsTheAdmins() {
    when(senderProvider.getIfAvailable()).thenReturn(sender);
    when(domainAdminUserService.findEmail("creator-sub")).thenThrow(new IllegalStateException("database down"));

    assertThatCode(() -> mailer(ADMINS).sendDeactivation(deactivated())).doesNotThrowAnyException();

    assertThat(sentMessage().getTo()).containsExactly("joris@headease.nl", "roland@headease.nl");
  }

  @Test
  void mailsACreatorWhoseIdIsTheirEmailAddress() {
    // in a realm that uses the email address as the subject, createdBy holds the creator's email address
    when(senderProvider.getIfAvailable()).thenReturn(sender);
    SmartService smartService = deactivated();
    smartService.setCreatedBy("patrick@example.com");

    mailer(ADMINS).sendDeactivation(smartService);

    assertThat(sentMessage().getTo()).containsExactly("joris@headease.nl", "roland@headease.nl", "patrick@example.com");
  }
}
