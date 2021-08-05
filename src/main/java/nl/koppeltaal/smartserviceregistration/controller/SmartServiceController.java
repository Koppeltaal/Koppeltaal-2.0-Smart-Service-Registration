package nl.koppeltaal.smartserviceregistration.controller;

import java.util.UUID;
import javax.servlet.http.HttpSession;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("smartService")
public class SmartServiceController {

  private final SmartServiceService smartServiceService;

  public SmartServiceController(SmartServiceService smartServiceService) {
    this.smartServiceService = smartServiceService;
  }

  @PostMapping(value = "status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String registerNewServiceRequest(@RequestParam UUID id, @RequestParam SmartServiceStatus status, HttpSession session) {
    smartServiceService.updateSmartServiceStatus(id, status, (String) session.getAttribute("user"));

    return "redirect:/";
  }

  @PostMapping(value = "name", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String registerNewServiceRequest(@RequestParam UUID id, @RequestParam String name, HttpSession session) {
    smartServiceService.updateSmartServiceName(id, name, (String) session.getAttribute("user"));

    return "redirect:/";
  }

  @PostMapping(value = "role", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String registerNewServiceRequest(@RequestParam UUID id, @RequestParam UUID roleId, HttpSession session) {
    smartServiceService.updateSmartServiceRole(id, roleId, (String) session.getAttribute("user"));

    return "redirect:/";
  }
}
