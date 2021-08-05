/*
 * Copyright (c) Stichting Koppeltaal 2021.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nl.koppeltaal.smartserviceregistration.controller;

import java.util.UUID;
import javax.servlet.http.HttpSession;
import nl.koppeltaal.smartserviceregistration.dto.PermissionDto;
import nl.koppeltaal.smartserviceregistration.model.Role;
import nl.koppeltaal.smartserviceregistration.service.RoleService;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("roles")
public class RoleController {

  private final RoleService roleService;
  private final SmartServiceService smartServiceService;

  public RoleController(RoleService roleService,
      SmartServiceService smartServiceService) {
    this.roleService = roleService;
    this.smartServiceService = smartServiceService;
  }

  @GetMapping
  public String showRoles(HttpSession session, Model model) {

    model.addAttribute("user", session.getAttribute("user"));
    model.addAttribute("roles", roleService.findAll());
    model.addAttribute("newRole", new Role());

    return "roles";
  }

  @PostMapping
  public String saveRole(@ModelAttribute Role role, Model model, HttpSession session) {

    Role persistedRole = roleService.save(role);

    return "redirect:/roles/edit?id=" + persistedRole.getId();
  }

  @GetMapping("edit")
  public String editRole(@RequestParam("id") UUID roleId, Model model, HttpSession session) {

    model.addAttribute("user", session.getAttribute("user"));

    final Role role = roleService.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown smart service id"));
    model.addAttribute("role", role);

    final PermissionDto permission = new PermissionDto();
    permission.setRole(role);
    model.addAttribute("newPermission", permission);

    model.addAttribute("smartServices", smartServiceService.findAll());

    return "edit_role";
  }
}
