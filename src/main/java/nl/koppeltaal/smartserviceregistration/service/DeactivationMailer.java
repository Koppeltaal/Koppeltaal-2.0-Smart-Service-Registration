package nl.koppeltaal.smartserviceregistration.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.util.EmailUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Mails about automatically deactivated applications, to the suspension admins and, for a deactivation, the creator.
 * Spring Boot only creates a {@link JavaMailSender} when spring.mail.host is set; without one the mail is logged
 * instead, and a mail never undoes a deactivation.
 */
@Component
public class DeactivationMailer {

  private static final Logger LOG = LoggerFactory.getLogger(DeactivationMailer.class);

  private final ObjectProvider<JavaMailSender> mailSender;
  private final DomainAdminUserService domainAdminUserService;
  private final SuspensionAdmins suspensionAdmins;
  private final String from;
  private final String replyTo;

  public DeactivationMailer(ObjectProvider<JavaMailSender> mailSender, DomainAdminUserService domainAdminUserService,
      SuspensionAdmins suspensionAdmins, @Value("${deactivation.mail.from:}") String from,
      @Value("${deactivation.mail.reply-to:}") String replyTo) {
    this.mailSender = mailSender;
    this.domainAdminUserService = domainAdminUserService;
    this.suspensionAdmins = suspensionAdmins;
    this.from = from;
    this.replyTo = replyTo;
  }

  public void sendDeactivation(SmartService smartService) {
    // keyed case-insensitively, so a creator who is an admin gets one copy
    final Map<String, String> recipients = new LinkedHashMap<>();
    suspensionAdmins.emails().forEach(email -> recipients.put(email, email));
    creatorEmail(smartService).map(String::trim)
        .ifPresent(email -> recipients.putIfAbsent(email.toLowerCase(Locale.ROOT), email));

    final String subject = "Koppeltaal: applicatie " + name(smartService) + " is automatisch gedeactiveerd";
    send(new ArrayList<>(recipients.values()), subject, String.join("\n",
        "De Koppeltaal FHIR-server heeft de applicatie \"" + name(smartService) + "\" (client_id "
            + smartService.getClientId() + ") automatisch gedeactiveerd, omdat de client een fout bevat: de applicatie"
            + " bleef verzoeken versturen die de FHIR-server al had geweigerd.",
        "",
        "Reden: " + smartService.getDeactivationReason(),
        "",
        "Zolang de applicatie gedeactiveerd is, krijgt zij geen nieuwe access tokens en weigert de FHIR-server haar"
            + " verzoeken.",
        "",
        "Wat moet u doen? Herstel de fout, zodat de applicatie een geweigerd verzoek niet ongewijzigd opnieuw verstuurt"
            + " (TOP-KT-012b). Zet de applicatie daarna in domain-admin weer op Approved.",
        "",
        "Blijft de applicatie geweigerde verzoeken herhalen, dan kan Koppeltaal beheer haar schorsen. Een schorsing kan"
            + " alleen beheer opheffen.",
        "",
        "Vragen? Neem contact op met Koppeltaal beheer"
            + (StringUtils.isNotBlank(replyTo) ? " via " + replyTo + " of beantwoord deze e-mail" : "") + "."));
  }

  public void sendReport(SmartService smartService, String reason) {
    final String subject = "Koppeltaal: applicatie " + name(smartService) + " zou automatisch gedeactiveerd worden";
    send(suspensionAdmins.emails(), subject, String.join("\n",
        "De Koppeltaal FHIR-server zou de applicatie \"" + name(smartService) + "\" (client_id "
            + smartService.getClientId() + ") automatisch deactiveren, maar staat in de rapportagemodus. Er is niets"
            + " geblokkeerd.",
        "",
        "Reden: " + reason));
  }

  /** The deactivation mail is due only once, so a failing lookup must not cost the admins their mail. */
  private Optional<String> creatorEmail(SmartService smartService) {
    // createdBy is the creator's sub, which some realms fill with the email address itself
    if (EmailUtils.isEmailAddress(smartService.getCreatedBy())) {
      return Optional.of(smartService.getCreatedBy());
    }
    try {
      return domainAdminUserService.findEmail(smartService.getCreatedBy());
    } catch (RuntimeException e) {
      LOG.error(String.format("Failed to look up the creator of client_id [%s]; mailing the suspension admins only",
          smartService.getClientId()), e);
      return Optional.empty();
    }
  }

  /** The name comes from the vendor; normalizing the whitespace keeps line breaks out of the subject. */
  private static String name(SmartService smartService) {
    return StringUtils.defaultIfBlank(StringUtils.normalizeSpace(smartService.getName()), smartService.getClientId());
  }

  private void send(List<String> recipients, String subject, String text) {
    final JavaMailSender sender = mailSender.getIfAvailable();
    if (sender == null || recipients.isEmpty()) {
      LOG.warn("Not sending mail [{}]: {}", subject, sender == null ? "no mail server configured" : "no recipients");
      return;
    }
    final SimpleMailMessage message = new SimpleMailMessage();
    if (StringUtils.isNotBlank(from)) {
      message.setFrom(from);
    }
    if (StringUtils.isNotBlank(replyTo)) {
      message.setReplyTo(replyTo);
    }
    message.setTo(recipients.toArray(String[]::new));
    message.setSubject(subject);
    message.setText(text);
    try {
      sender.send(message);
      LOG.info("Sent mail [{}] to [{}] recipient(s)", subject, recipients.size());
    } catch (RuntimeException e) {
      LOG.error(String.format("Failed to send mail [%s]", subject), e);
    }
  }
}
