package nl.koppeltaal.smartserviceregistration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * The defaults agreed for suspensions and deactivation mails. The test resources hold another application.properties,
 * hence the explicit path.
 */
class SuspensionConfigurationTest {

  private static Properties application() throws Exception {
    return PropertiesLoaderUtils.loadProperties(new FileSystemResource("src/main/resources/application.properties"));
  }

  @Test
  void onlyJorisAndRolandCanSuspendByDefault() throws Exception {
    assertThat(application().getProperty("suspension.admins")).isEqualTo("joris@headease.nl,roland@headease.nl");
  }

  @Test
  void repliesToDeactivationMailsGoToInfo() throws Exception {
    assertThat(application().getProperty("deactivation.mail.reply-to")).isEqualTo("info@headease.nl");
  }
}
