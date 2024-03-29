/*
 * Copyright (c) Stichting Koppeltaal 2021.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nl.koppeltaal.smartserviceregistration.jwt;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PublicKey;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 *
 */
@Component
public class JwtValidationService {

	private final JwkProviderFactory jwkProviderFactory;

	public JwtValidationService(JwkProviderFactory jwkProviderFactory) {
		this.jwkProviderFactory = jwkProviderFactory;
	}

	/**
	 * Unfortunately, this implementation of JWT has no helper method for selecting the right
	 * algorithm from the header. The public key must match the algorithm type (RSA or EC), but
	 * the size of the hash algorithm can vary.
	 *
	 * @param publicKey, the public key
	 * @param algorithmName, name of the algorithm
	 * @return in instance of the {@link Algorithm} class.
	 * @throws IllegalArgumentException if the algorithmName is not one of RS{256,384,512} or ES{256,384,512}
	 */
	private static Algorithm getValidationAlgorithm(PublicKey publicKey, String algorithmName) throws IllegalArgumentException {
		if (StringUtils.startsWith(algorithmName, "RS")) {
			algorithmName = "RSA" + StringUtils.removeStart(algorithmName, "RS");
		}
		if (StringUtils.startsWith(algorithmName, "EC")) {
			algorithmName = "ECDSA" + StringUtils.removeStart(algorithmName, "EC");
		}
		Method[] declaredMethods = Algorithm.class.getDeclaredMethods();
		for (Method declaredMethod : declaredMethods) {
			if (declaredMethod.getParameterCount() == 2 && StringUtils.equals(declaredMethod.getName(), algorithmName)) {
				try {
					return (Algorithm) declaredMethod.invoke(null, new Object[]{publicKey, null});
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new IllegalArgumentException(String.format("Problem calling method %s", declaredMethod.toString()));
				}
			}
		}
		throw new IllegalArgumentException(String.format("Unsupported algorithm %s", algorithmName));
	}

	@SuppressWarnings("UnusedReturnValue")
	public DecodedJWT validate(String token, String audience) throws JwkException {
		return validate(token, audience, 10);
	}

	public DecodedJWT validate(String token, String audience, int leeway) throws JwkException {
		// Get the algorithm name from the JWT.
		DecodedJWT decode = JWT.decode(token);
		String algorithmName = decode.getAlgorithm();
		// Lookup the issuer.
		String issuer = decode.getIssuer();

		JwkProvider provider = jwkProviderFactory.getJwkProvider(issuer);
		Jwk jwk = provider.get(decode.getKeyId());
		Assert.isTrue(jwk != null, String.format("Unable to locate public key for issuer %s", issuer));

		// Get the algorithm from the public key and algorithm name.
		Algorithm algorithm = getValidationAlgorithm(jwk.getPublicKey(), algorithmName);

		// Decode and verify the token.
		Verification verification = JWT.require(algorithm)
				.withIssuer(issuer) // Make sure to require yourself to be the audience.
				.acceptLeeway(leeway);
		if (StringUtils.isNotEmpty(audience)) {
			verification = verification.withAudience(audience); // Make sure to require yourself to be the audience.
		}
		try {
			return verification
					.build()
					.verify(token);
		} catch (Throwable e) {
			Date now = new Date();
			System.out.println("The time is: " + now);
			throw e;
		}
	}

}
