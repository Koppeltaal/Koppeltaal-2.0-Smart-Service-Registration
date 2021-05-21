package nl.koppeltaal.smartserviceregistration.controller;

import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.servlet.http.HttpSession;
import nl.koppeltaal.smartserviceregistration.exception.DuplicateJwksEndpointException;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("register")
public class RegisterSmartServiceController {

  private final SmartServiceService smartServiceService;

  public RegisterSmartServiceController(SmartServiceService smartServiceService) {
    this.smartServiceService = smartServiceService;
  }

  @GetMapping
  public String serveHtml(Model model) {

    final Set<String> registeredEndpoints = StreamSupport
        .stream(smartServiceService.findAll().spliterator(), false)
        .map(SmartService::getJwksEndpoint)
        .map(URL::toString)
        .collect(Collectors.toSet());

    model.addAttribute("registeredEndpoints", registeredEndpoints);

    return "register";
  }

  @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String registerNewServiceRequest(@RequestParam URL jwksEndpoint, HttpSession session) {
    smartServiceService.registerNewService(jwksEndpoint, (String) session.getAttribute("user"));
    return "redirect:/";
  }

  @ExceptionHandler(DuplicateJwksEndpointException.class)
  public String handleUniqueConstraint(Model model, Exception exception) {

    model.addAttribute("error", exception);

    return serveHtml(model);
  }
}
