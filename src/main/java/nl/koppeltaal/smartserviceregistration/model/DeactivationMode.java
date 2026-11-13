package nl.koppeltaal.smartserviceregistration.model;

/**
 * How the FHIR server wants a detected client handled: {@link #ENFORCE} deactivates it, {@link #REPORT} only tells beheer.
 */
public enum DeactivationMode {
  ENFORCE, REPORT
}
