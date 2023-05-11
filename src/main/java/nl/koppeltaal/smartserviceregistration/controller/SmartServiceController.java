package nl.koppeltaal.smartserviceregistration.controller;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.servlet.http.HttpSession;

import nl.koppeltaal.smartserviceregistration.config.SmartServiceConfiguration;
import nl.koppeltaal.smartserviceregistration.exception.SmartServiceRegistrationException;
import nl.koppeltaal.smartserviceregistration.model.SmartService;
import nl.koppeltaal.smartserviceregistration.model.SmartServiceStatus;
import nl.koppeltaal.smartserviceregistration.repository.IdentityProviderRepository;
import nl.koppeltaal.smartserviceregistration.service.SmartServiceService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("smart-service")
public class SmartServiceController {

  private final SmartServiceService smartServiceService;
  private final SmartServiceConfiguration smartServiceConfiguration;

  private final IdentityProviderRepository identityProviderRepository;

  public SmartServiceController(SmartServiceService smartServiceService, SmartServiceConfiguration smartServiceConfiguration, IdentityProviderRepository identityProviderRepository) {

    this.smartServiceService = smartServiceService;
    this.smartServiceConfiguration = smartServiceConfiguration;
    this.identityProviderRepository = identityProviderRepository;
  }

  @GetMapping
  public String serveHtml(Model model, HttpSession session) {
    return serveHtml(null, model, session);
  }

  @GetMapping("{id}")
  public String serveHtml(@PathVariable UUID id, Model model, HttpSession session) {

    final Set<String> registeredEndpoints = StreamSupport
            .stream(smartServiceService.findAll().spliterator(), false)
            .map(SmartService::getJwksEndpoint)
            .filter(Objects::nonNull)
            .map(URL::toString)
            .collect(Collectors.toSet());

    model.addAttribute("idps", identityProviderRepository.findByOrderByCreatedOnDesc());
    model.addAttribute("registeredEndpoints", registeredEndpoints);
    model.addAttribute("registeredEndpoints", registeredEndpoints);
    model.addAttribute("allowHttpHosts", smartServiceConfiguration.isAllowHttpHosts());
    model.addAttribute("user", session.getAttribute("user"));


    Optional<SmartService> smartServiceOptional = id != null ? smartServiceService.findById(id) : Optional.empty();

    model.addAttribute("client", smartServiceOptional.orElse(new SmartService()));

    return "smart-service";
  }

  @PostMapping
  public String upsert(@ModelAttribute SmartService smartService, HttpSession session) {
    smartService.setCreatedBy((String) session.getAttribute("user"));

    smartServiceService.upsert(smartService);

    return "redirect:/";
  }

  @DeleteMapping("{id}")
  @ResponseBody
  public void delete(@PathVariable UUID id, HttpSession session) {
    smartServiceService.delete(id, (String) session.getAttribute("user"));
  }

  @ExceptionHandler(SmartServiceRegistrationException.class)
  public String handleError(Model model, HttpSession session, SmartServiceRegistrationException exception) {

    model.addAttribute("error", exception);

    SmartService smartService = exception.getSmartService();

    return smartService != null ? serveHtml(smartService.getId(), model, session) : serveHtml(model, session);
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
