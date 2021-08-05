package nl.koppeltaal.smartserviceregistration.controller;

import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.servlet.http.HttpSession;
import nl.koppeltaal.smartserviceregistration.exception.SmartServiceRegistrationException;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("register")
public class RegisterSmartServiceController {

  private final SmartServiceService smartServiceService;

  public RegisterSmartServiceController(SmartServiceService smartServiceService) {
    this.smartServiceService = smartServiceService;
  }

  @GetMapping
  public String serveHtml(Model model, HttpSession session) {

    final Set<String> registeredEndpoints = StreamSupport
        .stream(smartServiceService.findAll().spliterator(), false)
        .map(SmartService::getJwksEndpoint)
        .filter(Objects::nonNull)
        .map(URL::toString)
        .collect(Collectors.toSet());

    model.addAttribute("registeredEndpoints", registeredEndpoints);
    model.addAttribute("user", session.getAttribute("user"));

    return "register";
  }

  @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String registerNewServiceRequest(@RequestParam String jwksEndpoint, @RequestParam String name, @RequestParam String publicKey, HttpSession session) {
    smartServiceService.registerNewService(jwksEndpoint, name, publicKey, (String) session.getAttribute("user"));
    return "redirect:/";
  }

  @DeleteMapping("{id}")
  @ResponseBody
  public void delete(@PathVariable UUID id, HttpSession session) {
    smartServiceService.delete(id, (String) session.getAttribute("user"));
  }

  @ExceptionHandler(SmartServiceRegistrationException.class)
  public String handleError(Model model, HttpSession session, Exception exception) {

    model.addAttribute("error", exception);

    return serveHtml(model, session);
  }
}
