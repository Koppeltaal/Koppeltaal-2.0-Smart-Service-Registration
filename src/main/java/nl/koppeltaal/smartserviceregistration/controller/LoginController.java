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
import nl.koppeltaal.smartserviceregistration.service.DomainAdminUserService;
import nl.koppeltaal.smartserviceregistration.util.EmailUtils;
import nl.koppeltaal.smartserviceregistration.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

	final OidcClientService oidcClientService;
	final DomainAdminUserService domainAdminUserService;

	public LoginController(OidcClientService oidcClientService, DomainAdminUserService domainAdminUserService) {
		this.oidcClientService = oidcClientService;
		this.domainAdminUserService = domainAdminUserService;
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
		// a reused session must not keep the email, and with it the admin rights, of an earlier login
		httpSession.removeAttribute("email");
		httpSession.setAttribute("user", userReference);
		recordLogin(httpSession, userReference, tokenStorage);
		return "redirect:/";

	}

	/**
	 * Keeps the login email in the session, which decides who is a suspension admin, and records it for the mail to the
	 * creator of a deactivated application.
	 */
	private void recordLogin(HttpSession httpSession, String userReference, SessionTokenStorage tokenStorage) {
		try {
			String identifier = oidcClientService.getUserIdentifierFromCredentials(tokenStorage);
			// the email claim, or without one the sub, which some realms fill with the email address
			if (EmailUtils.isEmailAddress(identifier)) {
				httpSession.setAttribute("email", identifier);
			}
			domainAdminUserService.recordLogin(userReference, identifier);
		} catch (JwkException | RuntimeException e) {
			LOG.warn(String.format("Failed to record the login of user [%s], continuing", userReference), e);
		}
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
