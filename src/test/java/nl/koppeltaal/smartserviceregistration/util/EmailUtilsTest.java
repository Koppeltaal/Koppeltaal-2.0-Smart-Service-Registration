package nl.koppeltaal.smartserviceregistration.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailUtilsTest {

  @Test
  void recognizesAnEmailAddress() {
    assertThat(EmailUtils.isEmailAddress("joris@headease.nl")).isTrue();
    assertThat(EmailUtils.isEmailAddress("patrick.berenschot@example.com")).isTrue();
  }

  @Test
  void rejectsOpaqueSubsAndOtherText() {
    assertThat(EmailUtils.isEmailAddress("sub-1")).isFalse();
    assertThat(EmailUtils.isEmailAddress("1d4f1b2e-6b1c-4c0e-9d1a-2f3e4d5c6b7a")).isFalse();
    assertThat(EmailUtils.isEmailAddress("Joris <joris@headease.nl>")).isFalse();
    assertThat(EmailUtils.isEmailAddress("a@b@c")).isFalse();
    assertThat(EmailUtils.isEmailAddress("team:a@example.com,b@example.com;")).isFalse();
    assertThat(EmailUtils.isEmailAddress("team:;")).isFalse();
    assertThat(EmailUtils.isEmailAddress("")).isFalse();
    assertThat(EmailUtils.isEmailAddress(null)).isFalse();
  }
}
