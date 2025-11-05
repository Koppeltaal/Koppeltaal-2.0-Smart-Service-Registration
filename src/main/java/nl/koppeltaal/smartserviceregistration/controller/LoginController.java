/*
 * Copyright (c) Stichting Koppeltaal 2021.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nl.koppeltaal.smartserviceregistration.controller;

import com.auth0.jwk.JwkException;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import nl.koppeltaal.smartserviceregistration.dto.AuthorizationUrlDto;
import nl.koppeltaal.smartserviceregistration.oidc.service.OidcClientService;
import nl.koppeltaal.smartserviceregistration.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 *
 */
@Controller

public class LoginController {

	final OidcClientService oidcClientService;

	public LoginController(OidcClientService oidcClientService) {
		this.oidcClientService = oidcClientService;
	}

	@GetMapping("code_response")
	public String codeResponse(HttpSession httpSession, HttpServletRequest request, String code, String state) throws IOException, JwkException {
		String sessionState = (String)httpSession.getAttribute("state");
		if (StringUtils.isEmpty(sessionState)) {
			return "redirect:/login";
		}
		if(!sessionState.equals(state)) throw new IllegalStateException("unexpected state");
		SessionTokenStorage tokenStorage = new SessionTokenStorage(httpSession);

		oidcClientService.getIdToken(code, UrlUtils.getServerUrl("/code_response", request), tokenStorage);

		String userReference = oidcClientService.getUserIdFromCredentials(tokenStorage);
		httpSession.setAttribute("user", userReference);
		return "redirect:/";

	}

	@GetMapping("/login")
	public View login(HttpSession httpSession, RedirectAttributes redirectAttributes, HttpServletRequest request) {
		AuthorizationUrlDto authorizationUrl = oidcClientService.getAuthorizationUrl(UrlUtils.getServerUrl("", request), UrlUtils.getServerUrl("/code_response", request));
		httpSession.setAttribute("state", authorizationUrl.getState());
		redirectAttributes.addAllAttributes(authorizationUrl.getParameters());
		return new RedirectView(authorizationUrl.getUrl());
	}

	@SuppressWarnings("SameReturnValue")
	@PostMapping("/logout")
	public String logout(HttpSession httpSession) {
		SessionTokenStorage tokenStorage = new SessionTokenStorage(httpSession);
		tokenStorage.clear();
		httpSession.invalidate();
		return "redirect:/login";
	}

}
