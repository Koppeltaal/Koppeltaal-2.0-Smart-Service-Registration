package nl.koppeltaal.smartserviceregistration.model;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.Type;

@MappedSuperclass
public class DbEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "created_on")
  private LocalDateTime createdOn = LocalDateTime.now();

  @Column(name = "created_by")
  private String createdBy;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LocalDateTime getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(LocalDateTime createdOn) {
    this.createdOn = createdOn;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  public String toString() {
    return "DbEntity{" +
        "id=" + id +
        ", createdOn=" + createdOn +
        ", createdBy='" + createdBy + '\'' +
        '}';
  }
}
