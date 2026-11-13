package nl.koppeltaal.smartserviceregistration.service;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** JavaMail waits forever by default; the configured timeouts must cut off a stalled mail server. */
class SmtpTimeoutTest {

  private static final String PREFIX = "spring.mail.properties.";

  @Test
  void aStalledMailServerCannotHoldUpTheDeactivation() throws Exception {
    // accepts connections in its backlog, but never greets
    try (ServerSocket stalledMailServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      JavaMailSenderImpl sender = new JavaMailSenderImpl();
      sender.setHost(stalledMailServer.getInetAddress().getHostAddress());
      sender.setPort(stalledMailServer.getLocalPort());
      sender.setJavaMailProperties(configuredJavaMailProperties());
      @SuppressWarnings("unchecked")
      ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
      when(provider.getIfAvailable()).thenReturn(sender);
      DeactivationMailer mailer = new DeactivationMailer(provider, mock(DomainAdminUserService.class),
          new SuspensionAdmins("joris@headease.nl"), "noreply@koppeltaal.nl", "info@headease.nl");

      assertTimeoutPreemptively(Duration.ofSeconds(20),
          () -> mailer.sendReport(DeactivationMailerTest.deactivated(), "Rejected 300 write requests"));
    }
  }

  /** The spring.mail.properties.* entries of the application, as Spring Boot hands them to JavaMail. */
  private static Properties configuredJavaMailProperties() throws IOException {
    // the test resources hold another application.properties, hence the explicit path
    Properties application = PropertiesLoaderUtils.loadProperties(new FileSystemResource("src/main/resources/application.properties"));
    Properties javaMail = new Properties();
    application.stringPropertyNames().stream()
        .filter(name -> name.startsWith(PREFIX))
        .forEach(name -> javaMail.setProperty(name.substring(PREFIX.length()), application.getProperty(name)));
    return javaMail;
  }
}
