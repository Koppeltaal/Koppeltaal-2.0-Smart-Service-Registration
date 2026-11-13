package nl.koppeltaal.smartserviceregistration.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The users who may suspend an application and lift a suspension, recognized by the email they log in with. They also
 * get the deactivation mails. Without any configured, nobody can suspend or lift a suspension.
 */
@Component
public class SuspensionAdmins {

  private static final Logger LOG = LoggerFactory.getLogger(SuspensionAdmins.class);

  private final List<String> emails;

  /**
   * @param emails comma-separated
   */
  public SuspensionAdmins(@Value("${suspension.admins:}") String emails) {
    this.emails = Arrays.stream(StringUtils.defaultString(emails).split(","))
        .map(SuspensionAdmins::normalize)
        .filter(StringUtils::isNotBlank)
        .distinct()
        .toList();
    if (this.emails.isEmpty()) {
      LOG.warn("No suspension admins configured (suspension.admins); nobody can suspend an application or lift a "
          + "suspension, and deactivation mails go to the creator only");
    }
  }

  public boolean isAdmin(String email) {
    return StringUtils.isNotBlank(email) && emails.contains(normalize(email));
  }

  /** The configured addresses, normalized, each once and in their configured order. */
  public List<String> emails() {
    return emails;
  }

  private static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
