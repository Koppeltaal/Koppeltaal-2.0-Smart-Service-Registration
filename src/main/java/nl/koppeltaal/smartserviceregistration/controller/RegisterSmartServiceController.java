package nl.koppeltaal.smartserviceregistration.controller;

import java.net.URL;
import javax.servlet.http.HttpSession;
import nl.koppeltaal.smartserviceregistration.service.RegisterSmartServiceService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("register")
public class RegisterSmartServiceController {

  private final RegisterSmartServiceService registerSmartServiceService;

  public RegisterSmartServiceController(RegisterSmartServiceService registerSmartServiceService) {
    this.registerSmartServiceService = registerSmartServiceService;
  }

  @GetMapping
  public String serveHtml() {
    return "register";
  }

  @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String registerNewServiceRequest(@RequestParam URL jwksEndpoint, HttpSession session) {
    registerSmartServiceService.registerNewService(jwksEndpoint, (String) session.getAttribute("user"));
    return "redirect:/";
  }
}
