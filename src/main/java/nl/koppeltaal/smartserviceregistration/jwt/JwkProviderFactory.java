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
import com.auth0.jwk.UrlJwkProvider;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class JwkProviderFactory {
	final private static Map<String, JwkProvider> CACHE = new HashMap<>();

	public static JwkProvider getJwkProvider(String issuer) {
		if (CACHE.containsKey(issuer)) {
			return CACHE.get(issuer);
		}
		JwkProvider provider = new GuavaCachedJwkProvider(new UrlJwkProvider(issuer));
		CACHE.put(issuer, provider);
		return provider;
	}
}
