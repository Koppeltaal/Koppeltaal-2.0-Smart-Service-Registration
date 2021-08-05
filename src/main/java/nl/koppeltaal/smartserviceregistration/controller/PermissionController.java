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
import nl.koppeltaal.smartserviceregistration.exception.PermissionException;
import nl.koppeltaal.smartserviceregistration.model.Permission;
import nl.koppeltaal.smartserviceregistration.model.Role;
import nl.koppeltaal.smartserviceregistration.repository.PermissionRepository;
import nl.koppeltaal.smartserviceregistration.service.RoleService;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("permission")
public class PermissionController {

  private final PermissionRepository repository;
  private final RoleService roleService;
  private final SmartServiceService smartServiceService;

  public PermissionController(
      PermissionRepository repository,
      RoleService roleService, SmartServiceService smartServiceService) {
    this.repository = repository;
    this.roleService = roleService;
    this.smartServiceService = smartServiceService;
  }

  @GetMapping
  public String newPermission(@RequestParam UUID roleId, Model model, HttpSession session) {

    model.addAttribute("user", session.getAttribute("user"));

    final Role role = roleService.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown role id"));

    final PermissionDto newPermission = new PermissionDto();
    newPermission.setRole(role);

    model.addAttribute("permission", newPermission);
    model.addAttribute("edit", false);
    model.addAttribute("smartServices", smartServiceService.findAll());

    return "edit_permission";
  }

  @GetMapping("edit")
  public String editPermission(@RequestParam UUID id, Model model, HttpSession session) {

    model.addAttribute("user", session.getAttribute("user"));

    final Permission permission = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Unknown permission id"));

    model.addAttribute("permission", PermissionDto.toPermissionDto(permission));
    model.addAttribute("edit", true);
    model.addAttribute("smartServices", smartServiceService.findAll());

    return "edit_permission";
  }

  @PostMapping
  public String addPermission(@ModelAttribute("permission") PermissionDto permissionDto, @RequestParam UUID roleId) {

    roleService.addOrUpdatePermission(permissionDto, roleId);

    return "redirect:/roles/edit?id=" + roleId;
  }

  @PostMapping(value = "delete", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String deletePermission(@RequestParam UUID permissionId, @RequestParam UUID roleId, Model model, HttpSession session) {

    roleService.deletePermission(permissionId);

    return "redirect:/roles/edit?id=" + roleId;
  }

  @ExceptionHandler(PermissionException.class)
  public String handleError(Model model, HttpSession session, PermissionException exception) {

    model.addAttribute("error", exception);

    final UUID permissionId = exception.getPermissionId();

    return permissionId != null ? editPermission(permissionId, model, session) : newPermission(
        exception.getRoleId(), model, session);
  }

}
