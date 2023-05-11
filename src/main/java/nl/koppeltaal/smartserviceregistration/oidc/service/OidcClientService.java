/*
 * Copyright (c) Stichting Koppeltaal 2021.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nl.koppeltaal.smartserviceregistration.oidc.service;

import com.auth0.jwk.JwkException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.koppeltaal.smartserviceregistration.dto.AuthorizationUrlDto;
import nl.koppeltaal.smartserviceregistration.jwt.JwtValidationService;
import nl.koppeltaal.smartserviceregistration.oidc.IdTokenResponse;
import nl.koppeltaal.smartserviceregistration.oidc.TokenStorage;
import nl.koppeltaal.smartserviceregistration.oidc.configuration.OidcConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 *
 */
@Service
public class OidcClientService {

	private final static Logger LOG = LoggerFactory.getLogger(OidcClientService.class);

	final JwtValidationService jwtValidationService;
	final OidcConfiguration oidcConfiguration;

	public OidcClientService(JwtValidationService jwtValidationService, OidcConfiguration oidcConfiguration) {
		this.jwtValidationService = jwtValidationService;
		this.oidcConfiguration = oidcConfiguration;
	}

	public AuthorizationUrlDto getAuthorizationUrl(String serverUrl, String redirectUrl) {
		AuthorizationUrlDto rv = new AuthorizationUrlDto();
		String authorizeUrl = oidcConfiguration.getAuthorizationUrl();
		rv.putParameter("response_type", "code");
		rv.putParameter("client_id", oidcConfiguration.getClientId());
		rv.putParameter("redirect_uri", redirectUrl);
		rv.putParameter("scope", "openid");
		String state = UUID.randomUUID().toString();
		rv.putParameter("state", state);
		rv.putParameter("aud", serverUrl);

		rv.setUrl(authorizeUrl);
		rv.setState(state);
		return rv;
	}

	public void getIdToken(String code, String redirectUri, TokenStorage tokenStorage) throws IOException {
		String tokenUrl = oidcConfiguration.getTokenUrl();
		try (CloseableHttpClient httpClient = createHttpClient()) {

			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("grant_type", "authorization_code"));
			params.add(new BasicNameValuePair("redirect_uri", redirectUri));
			params.add(new BasicNameValuePair("code", code));

			final HttpPost httpPost = new HttpPost(tokenUrl);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", oidcConfiguration.getClientId(), oidcConfiguration.getClientSecret()).getBytes(StandardCharsets.US_ASCII)));
			httpPost.setEntity(new UrlEncodedFormEntity(params));
			CloseableHttpResponse response = httpClient.execute(httpPost);
			try (InputStream in = response.getEntity().getContent()) {
				String res = IOUtils.toString(in, StandardCharsets.UTF_8);
				LOG.debug("Got response: {}", res);
				ObjectMapper objectMapper = new ObjectMapper();
				tokenStorage.updateToken(objectMapper.readValue(res, IdTokenResponse.class));
			}
		}
	}

	public String getUserIdFromCredentials(TokenStorage tokenStorage) throws JwkException {
		DecodedJWT token = getIdToken(tokenStorage);
		return token.getSubject();

	}

	public String getUserIdentifierFromCredentials(TokenStorage tokenStorage) throws JwkException {
		DecodedJWT token = getIdToken(tokenStorage);
		String email = token.getClaim("email").asString();
		if (StringUtils.isNotEmpty(email)) {
			return email;
		}
		return token.getSubject();

	}

	private CloseableHttpClient createHttpClient() {
		return HttpClients.createDefault();
	}

	private DecodedJWT getIdToken(TokenStorage tokenStorage) throws JwkException {
		return jwtValidationService.validate(tokenStorage.getIdToken().getIdToken(), oidcConfiguration.getClientId(), 60);
	}

}
