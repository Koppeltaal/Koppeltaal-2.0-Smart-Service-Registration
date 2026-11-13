package nl.koppeltaal.smartserviceregistration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import nl.koppeltaal.smartserviceregistration.model.DomainAdminUser;
import nl.koppeltaal.smartserviceregistration.repository.DomainAdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DomainAdminUserServiceTest {

  @Mock private DomainAdminUserRepository repository;
  @InjectMocks private DomainAdminUserService service;

  @Test
  void recordsTheEmailOfANewUser() {
    when(repository.findById("sub-1")).thenReturn(Optional.empty());

    service.recordLogin("sub-1", "vendor@example.com");

    ArgumentCaptor<DomainAdminUser> saved = ArgumentCaptor.forClass(DomainAdminUser.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getSub()).isEqualTo("sub-1");
    assertThat(saved.getValue().getEmail()).isEqualTo("vendor@example.com");
    assertThat(saved.getValue().getLastLoginOn()).isNotNull();
  }

  @Test
  void keepsTheKnownEmailWhenTheTokenHasNone() {
    DomainAdminUser known = new DomainAdminUser("sub-1");
    known.setEmail("vendor@example.com");
    when(repository.findById("sub-1")).thenReturn(Optional.of(known));

    // without an email claim the identifier is the sub
    service.recordLogin("sub-1", "sub-1");

    verify(repository).save(known);
    assertThat(known.getEmail()).isEqualTo("vendor@example.com");
    assertThat(known.getLastLoginOn()).isNotNull();
  }

  @Test
  void findsTheEmailOfAKnownUser() {
    DomainAdminUser known = new DomainAdminUser("sub-1");
    known.setEmail("vendor@example.com");
    when(repository.findById("sub-1")).thenReturn(Optional.of(known));

    assertThat(service.findEmail("sub-1")).contains("vendor@example.com");
  }

  @Test
  void findsNoEmailForAnUnknownOrMissingUser() {
    when(repository.findById("sub-2")).thenReturn(Optional.empty());

    assertThat(service.findEmail("sub-2")).isEmpty();
    assertThat(service.findEmail(null)).isEmpty();
  }
}
