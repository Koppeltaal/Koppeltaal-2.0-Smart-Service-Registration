package nl.koppeltaal.smartserviceregistration.util;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.apache.commons.lang3.StringUtils;

public final class EmailUtils {

  private EmailUtils() {
  }

  /**
   * A bare email address. Depending on the realm, the sub of a login is the user's email address or an opaque id.
   */
  public static boolean isEmailAddress(String value) {
    if (StringUtils.isBlank(value)) {
      return false;
    }
    try {
      final InternetAddress address = new InternetAddress(value, true);
      address.validate();
      return !address.isGroup() && value.equals(address.getAddress());
    } catch (AddressException e) {
      return false;
    }
  }
}
