package nl.koppeltaal.smartserviceregistration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *
 */

@Configuration
@ConfigurationProperties(prefix = "smartservice")
public class SmartServiceConfiguration {
	boolean allowHttpHosts = false;

	public boolean isAllowHttpHosts() {
		return allowHttpHosts;
	}

	public void setAllowHttpHosts(boolean allowHttpHosts) {
		this.allowHttpHosts = allowHttpHosts;
	}

}
