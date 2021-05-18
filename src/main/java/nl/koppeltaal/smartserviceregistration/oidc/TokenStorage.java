/*
 * Copyright (c) Stichting Koppeltaal 2021.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nl.koppeltaal.smartserviceregistration.oidc;

/**
 *
 */
public interface TokenStorage {
	void clear();

	IdTokenResponse getIdToken();

	void updateToken(IdTokenResponse token);
}
