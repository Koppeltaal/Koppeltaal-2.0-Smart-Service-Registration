/*
 * Copyright (c) Stichting Koppeltaal 2021.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nl.koppeltaal.smartserviceregistration.controller;

import javax.servlet.http.HttpSession;
import nl.koppeltaal.smartserviceregistration.service.RoleService;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  private final SmartServiceService smartServiceService;
  private final RoleService roleService;

  public HomeController(SmartServiceService smartServiceService,
      RoleService roleService) {
    this.smartServiceService = smartServiceService;
    this.roleService = roleService;
  }

  @GetMapping("/")
  public String showHome(HttpSession session, Model model) {

    model.addAttribute("smartServices", smartServiceService.findAll());
    model.addAttribute("roles", roleService.findAll());
    model.addAttribute("user", session.getAttribute("user"));

    return "index";
  }

}
