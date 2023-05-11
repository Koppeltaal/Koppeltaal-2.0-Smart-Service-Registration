package nl.koppeltaal.smartserviceregistration.controller;

import nl.koppeltaal.smartserviceregistration.model.IdentityProvider;
import nl.koppeltaal.smartserviceregistration.repository.IdentityProviderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("idp")
public class IdentityProviderController {

  private final IdentityProviderRepository repository;

  public IdentityProviderController(IdentityProviderRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public String list(Model model, HttpSession session) {
    model.addAttribute("idps", repository.findByOrderByCreatedOnDesc());
    return "idps";
  }

  @GetMapping("details")
  public String serveHtml(Model model) {
    return serveHtml(null, model);
  }


  @GetMapping("details/{id}")
  public String serveHtml(@PathVariable UUID id, Model model) {

    Optional<IdentityProvider> idpOptional = id != null ? repository.findById(id) : Optional.empty();

    model.addAttribute("idp", idpOptional.orElse(new IdentityProvider()));

    return "idp_details";
  }

  @PostMapping("details")
  public String upsert(@ModelAttribute IdentityProvider identityProvider, HttpSession session) {
    identityProvider.setCreatedBy((String) session.getAttribute("user"));

    if(identityProvider.getId() != null) {
      IdentityProvider existingIdentityProvider = repository.findById(identityProvider.getId()).orElseThrow();

      existingIdentityProvider.setName(identityProvider.getName());
      existingIdentityProvider.setEndpoint(identityProvider.getEndpoint());
      existingIdentityProvider.setClientId(identityProvider.getClientId());
      existingIdentityProvider.setClientSecret(identityProvider.getClientSecret());
      existingIdentityProvider.setUsernameAttribute(identityProvider.getUsernameAttribute());

      repository.save(existingIdentityProvider);
    } else {
      repository.save(identityProvider);
    }

    return "redirect:/idp";
  }

  @DeleteMapping("{id}")
  @ResponseBody
  public void delete(@PathVariable UUID id) {

    repository.delete(repository.findById(id).orElseThrow());
  }

}
