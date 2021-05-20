package nl.koppeltaal.smartserviceregistration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import nl.koppeltaal.smartserviceregistration.SmartServiceRegistrationApplicationTests.Initializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = {Initializer.class})
class SmartServiceRegistrationApplicationTests {

  @Container
  private static final MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.25"));

  @Test
  void contextLoads() {
    assertTrue(MY_SQL_CONTAINER.isRunning());
  }

  static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
          "spring.datasource.url=" + MY_SQL_CONTAINER.getJdbcUrl(),
          "spring.datasource.username=" + MY_SQL_CONTAINER.getUsername(),
          "spring.datasource.password=" + MY_SQL_CONTAINER.getPassword()
      ).applyTo(configurableApplicationContext.getEnvironment());
    }
  }

}
