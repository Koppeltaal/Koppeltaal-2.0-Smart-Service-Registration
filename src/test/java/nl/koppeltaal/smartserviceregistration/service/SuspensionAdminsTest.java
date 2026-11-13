package nl.koppeltaal.smartserviceregistration.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SuspensionAdminsTest {

  @Test
  void recognizesTheConfiguredEmailsWhateverTheirCaseOrSpacing() {
    SuspensionAdmins admins = new SuspensionAdmins(" Joris@Headease.nl, roland@headease.nl ");

    assertThat(admins.isAdmin("joris@headease.nl")).isTrue();
    assertThat(admins.isAdmin("ROLAND@HEADEASE.NL")).isTrue();
    assertThat(admins.isAdmin("vendor@example.com")).isFalse();
  }

  @Test
  void nobodyIsAnAdminWithoutAnEmail() {
    SuspensionAdmins admins = new SuspensionAdmins("joris@headease.nl");

    assertThat(admins.isAdmin(null)).isFalse();
    assertThat(admins.isAdmin(" ")).isFalse();
  }

  @Test
  void nobodyIsAnAdminWhenNoneAreConfigured() {
    SuspensionAdmins admins = new SuspensionAdmins(" , ");

    assertThat(admins.isAdmin("joris@headease.nl")).isFalse();
    assertThat(admins.isAdmin("")).isFalse();
    assertThat(admins.emails()).isEmpty();
  }

  @Test
  void listsTheConfiguredEmailsOnceEachInTheirConfiguredOrder() {
    SuspensionAdmins admins = new SuspensionAdmins(" Joris@Headease.nl, roland@headease.nl, joris@headease.nl ");

    assertThat(admins.emails()).containsExactly("joris@headease.nl", "roland@headease.nl");
  }
}
