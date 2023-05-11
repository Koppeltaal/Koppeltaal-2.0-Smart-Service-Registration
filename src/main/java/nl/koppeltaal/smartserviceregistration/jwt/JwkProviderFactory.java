/*
 * Copyright (c) Stichting Koppeltaal 2021.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nl.koppeltaal.smartserviceregistration.jwt;

import com.auth0.jwk.GuavaCachedJwkProvider;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwk.UrlJwkProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Component
public class JwkProviderFactory {
	final private Map<String, JwkProvider> CACHE = new HashMap<>();

	@Value("#{${jwk.provider.factory.issuer.override}}")
	private Map<String,String> issuerOverrides;

	private final static Logger LOG = LoggerFactory.getLogger(JwkProviderFactory.class);

	public JwkProvider getJwkProvider(String issuer) {

		LOG.info("Getting JWK provider for issuer [{}]", issuer);

		if (CACHE.containsKey(issuer)) {
			return CACHE.get(issuer);
		}
		JwkProvider provider;

		try {
			provider = getJwksUrlFromOpenidConfiguration(issuer);
		} catch (URISyntaxException | IOException e) {
			//fallback to issuer + /.well-known/jwks.json
			provider = new GuavaCachedJwkProvider(new UrlJwkProvider(issuer));
			CACHE.put(issuer, provider);
		}

		return provider;
	}

	private JwkProvider getJwksUrlFromOpenidConfiguration(String issuer) throws URISyntaxException, IOException {
		JwkProvider provider;

		if(issuerOverrides.containsKey(issuer)) {
			String overrideValue = issuerOverrides.get(issuer);
			LOG.info("Overriding issuer [{}] with [{}]", issuer, overrideValue);
			issuer = overrideValue;
		}

		URL url = new URI(issuer + "/.well-known/openid-configuration").normalize().toURL();
		URLConnection connection = url.openConnection();
		connection.setRequestProperty("Accept", "application/json");
		ObjectReader reader = new ObjectMapper().readerFor(Map.class);

		InputStream inputStream = connection.getInputStream();
		byte[] bytes = ByteStreams.toByteArray(inputStream);
		Map<String, Object> discoveryPayload = reader.readValue(bytes);
		String jwksUri = (String) discoveryPayload.get("jwks_uri");

		URL jwksUrl = new URI(jwksUri).normalize().toURL();
		provider = new JwkProviderBuilder(jwksUrl)
				.build();
		return provider;
	}
}
