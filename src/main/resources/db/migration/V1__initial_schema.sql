--
-- PostgreSQL database dump
--

-- Dumped from database version 17.5 (Homebrew)
-- Dumped by pg_dump version 17.5 (Homebrew)

--
-- Name: allowed_redirect; Type: TABLE; Schema: public; Owner: -
--
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_table_access_method = heap;

CREATE TABLE IF NOT EXISTS public.allowed_redirect (
    smart_service_id uuid NOT NULL,
    url character varying(255)
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: identity_provider; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.identity_provider (
    id uuid NOT NULL,
    created_by character varying(255),
    created_on timestamp(6) without time zone,
    client_id character varying(255),
    client_secret character varying(255),
    name character varying(255),
    openid_config_endpoint character varying(255),
    username_attribute character varying(255)
);


--
-- Name: permission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.permission (
    id uuid NOT NULL,
    created_by character varying(255),
    created_on timestamp(6) without time zone,
    operation character varying(255),
    resource_type character varying(255),
    scope character varying(255),
    role_id uuid NOT NULL,
    CONSTRAINT permission_operation_check CHECK (((operation)::text = ANY ((ARRAY['CREATE'::character varying, 'READ'::character varying, 'UPDATE'::character varying, 'DELETE'::character varying])::text[]))),
    CONSTRAINT permission_resource_type_check CHECK (((resource_type)::text = ANY ((ARRAY['Account'::character varying, 'ActivityDefinition'::character varying, 'AdverseEvent'::character varying, 'AllergyIntolerance'::character varying, 'Appointment'::character varying, 'AppointmentResponse'::character varying, 'AuditEvent'::character varying, 'Basic'::character varying, 'Binary'::character varying, 'BiologicallyDerivedProduct'::character varying, 'BodyStructure'::character varying, 'Bundle'::character varying, 'CapabilityStatement'::character varying, 'CarePlan'::character varying, 'CareTeam'::character varying, 'CatalogEntry'::character varying, 'ChargeItem'::character varying, 'ChargeItemDefinition'::character varying, 'Claim'::character varying, 'ClaimResponse'::character varying, 'ClinicalImpression'::character varying, 'CodeSystem'::character varying, 'Communication'::character varying, 'CommunicationRequest'::character varying, 'CompartmentDefinition'::character varying, 'Composition'::character varying, 'ConceptMap'::character varying, 'Condition'::character varying, 'Consent'::character varying, 'Contract'::character varying, 'Coverage'::character varying, 'CoverageEligibilityRequest'::character varying, 'CoverageEligibilityResponse'::character varying, 'DetectedIssue'::character varying, 'Device'::character varying, 'DeviceDefinition'::character varying, 'DeviceMetric'::character varying, 'DeviceRequest'::character varying, 'DeviceUseStatement'::character varying, 'DiagnosticReport'::character varying, 'DocumentManifest'::character varying, 'DocumentReference'::character varying, 'EffectEvidenceSynthesis'::character varying, 'Encounter'::character varying, 'Endpoint'::character varying, 'EnrollmentRequest'::character varying, 'EnrollmentResponse'::character varying, 'EpisodeOfCare'::character varying, 'EventDefinition'::character varying, 'Evidence'::character varying, 'EvidenceVariable'::character varying, 'ExampleScenario'::character varying, 'ExplanationOfBenefit'::character varying, 'FamilyMemberHistory'::character varying, 'Flag'::character varying, 'Goal'::character varying, 'GraphDefinition'::character varying, 'Group'::character varying, 'GuidanceResponse'::character varying, 'HealthcareService'::character varying, 'ImagingStudy'::character varying, 'Immunization'::character varying, 'ImmunizationEvaluation'::character varying, 'ImmunizationRecommendation'::character varying, 'ImplementationGuide'::character varying, 'InsurancePlan'::character varying, 'Invoice'::character varying, 'Library'::character varying, 'Linkage'::character varying, 'List'::character varying, 'Location'::character varying, 'Measure'::character varying, 'MeasureReport'::character varying, 'Media'::character varying, 'Medication'::character varying, 'MedicationAdministration'::character varying, 'MedicationDispense'::character varying, 'MedicationKnowledge'::character varying, 'MedicationRequest'::character varying, 'MedicationStatement'::character varying, 'MedicinalProduct'::character varying, 'MedicinalProductAuthorization'::character varying, 'MedicinalProductContraindication'::character varying, 'MedicinalProductIndication'::character varying, 'MedicinalProductIngredient'::character varying, 'MedicinalProductInteraction'::character varying, 'MedicinalProductManufactured'::character varying, 'MedicinalProductPackaged'::character varying, 'MedicinalProductPharmaceutical'::character varying, 'MedicinalProductUndesirableEffect'::character varying, 'MessageDefinition'::character varying, 'MessageHeader'::character varying, 'MolecularSequence'::character varying, 'NamingSystem'::character varying, 'NutritionOrder'::character varying, 'Observation'::character varying, 'ObservationDefinition'::character varying, 'OperationDefinition'::character varying, 'OperationOutcome'::character varying, 'Organization'::character varying, 'OrganizationAffiliation'::character varying, 'Parameters'::character varying, 'Patient'::character varying, 'PaymentNotice'::character varying, 'PaymentReconciliation'::character varying, 'Person'::character varying, 'PlanDefinition'::character varying, 'Practitioner'::character varying, 'PractitionerRole'::character varying, 'Procedure'::character varying, 'Provenance'::character varying, 'Questionnaire'::character varying, 'QuestionnaireResponse'::character varying, 'RelatedPerson'::character varying, 'RequestGroup'::character varying, 'ResearchDefinition'::character varying, 'ResearchElementDefinition'::character varying, 'ResearchStudy'::character varying, 'ResearchSubject'::character varying, 'RiskAssessment'::character varying, 'RiskEvidenceSynthesis'::character varying, 'Schedule'::character varying, 'SearchParameter'::character varying, 'ServiceRequest'::character varying, 'Slot'::character varying, 'Specimen'::character varying, 'SpecimenDefinition'::character varying, 'StructureDefinition'::character varying, 'StructureMap'::character varying, 'Subscription'::character varying, 'Substance'::character varying, 'SubstanceNucleicAcid'::character varying, 'SubstancePolymer'::character varying, 'SubstanceProtein'::character varying, 'SubstanceReferenceInformation'::character varying, 'SubstanceSourceMaterial'::character varying, 'SubstanceSpecification'::character varying, 'SupplyDelivery'::character varying, 'SupplyRequest'::character varying, 'Task'::character varying, 'TerminologyCapabilities'::character varying, 'TestReport'::character varying, 'TestScript'::character varying, 'ValueSet'::character varying, 'VerificationResult'::character varying, 'VisionPrescription'::character varying])::text[]))),
    CONSTRAINT permission_scope_check CHECK (((scope)::text = ANY ((ARRAY['ALL'::character varying, 'OWN'::character varying, 'GRANTED'::character varying])::text[])))
);


--
-- Name: permission_service_grant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.permission_service_grant (
    permission_id uuid NOT NULL,
    smart_service_id uuid NOT NULL
);


--
-- Name: role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.role (
    id uuid NOT NULL,
    created_by character varying(255),
    created_on timestamp(6) without time zone,
    name character varying(255)
);


--
-- Name: smart_service; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.smart_service (
    id uuid NOT NULL,
    created_by character varying(255),
    created_on timestamp(6) without time zone,
    client_id character varying(255),
    fhir_store_device_id character varying(255),
    jwks_endpoint character varying(255),
    name character varying(255),
    public_key character varying(512),
    status character varying(255),
    role_id uuid,
    CONSTRAINT smart_service_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: smart_service_patient_idp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.smart_service_patient_idp (
    smart_service_id uuid NOT NULL,
    identity_provider_id uuid NOT NULL
);


--
-- Name: smart_service_practitioner_idp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.smart_service_practitioner_idp (
    smart_service_id uuid NOT NULL,
    identity_provider_id uuid NOT NULL
);


--
-- Name: smart_service_related_person_idp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.smart_service_related_person_idp (
    smart_service_id uuid NOT NULL,
    identity_provider_id uuid NOT NULL
);


--
-- Name: smart_service client_id_index; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service
    ADD CONSTRAINT client_id_index UNIQUE (client_id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: identity_provider identity_provider_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.identity_provider
    ADD CONSTRAINT identity_provider_pkey PRIMARY KEY (id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: permission permission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.permission
    ADD CONSTRAINT permission_pkey PRIMARY KEY (id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: permission_service_grant permission_service_grant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.permission_service_grant
    ADD CONSTRAINT permission_service_grant_pkey PRIMARY KEY (permission_id, smart_service_id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: role role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service_patient_idp smart_service_patient_idp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service_patient_idp
    ADD CONSTRAINT smart_service_patient_idp_pkey PRIMARY KEY (smart_service_id, identity_provider_id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service smart_service_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service
    ADD CONSTRAINT smart_service_pkey PRIMARY KEY (id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service_practitioner_idp smart_service_practitioner_idp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service_practitioner_idp
    ADD CONSTRAINT smart_service_practitioner_idp_pkey PRIMARY KEY (smart_service_id, identity_provider_id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service_related_person_idp smart_service_related_person_idp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service_related_person_idp
    ADD CONSTRAINT smart_service_related_person_idp_pkey PRIMARY KEY (smart_service_id, identity_provider_id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service unique_jwks_endpoint; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service
    ADD CONSTRAINT unique_jwks_endpoint UNIQUE (jwks_endpoint);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: permission unique_permission; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.permission
    ADD CONSTRAINT unique_permission UNIQUE (role_id, resource_type, operation);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service unique_public_key; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service
    ADD CONSTRAINT unique_public_key UNIQUE (public_key);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: smart_service_related_person_idp fk1tqyevpmkgimqnt3o671odvpk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service_related_person_idp
    ADD CONSTRAINT fk1tqyevpmkgimqnt3o671odvpk FOREIGN KEY (smart_service_id) REFERENCES public.smart_service(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service_patient_idp fk3jw6x4kivase9vtge8nbw42yk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service_patient_idp
    ADD CONSTRAINT fk3jw6x4kivase9vtge8nbw42yk FOREIGN KEY (identity_provider_id) REFERENCES public.identity_provider(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: allowed_redirect fk4vnv1lt4xhpcy57hkeepfsfef; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.allowed_redirect
    ADD CONSTRAINT fk4vnv1lt4xhpcy57hkeepfsfef FOREIGN KEY (smart_service_id) REFERENCES public.smart_service(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: permission_service_grant fkcgjhhrn71uynab1031epbovrx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.permission_service_grant
    ADD CONSTRAINT fkcgjhhrn71uynab1031epbovrx FOREIGN KEY (permission_id) REFERENCES public.permission(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service fkcosi5jmx6d18vmwqhv2h3gmr0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service
    ADD CONSTRAINT fkcosi5jmx6d18vmwqhv2h3gmr0 FOREIGN KEY (role_id) REFERENCES public.role(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: permission_service_grant fkdc1aains9omcxwoulinqyvr7j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.permission_service_grant
    ADD CONSTRAINT fkdc1aains9omcxwoulinqyvr7j FOREIGN KEY (smart_service_id) REFERENCES public.smart_service(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service_patient_idp fkhqg7dyfxjble9mpmgygki43o1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service_patient_idp
    ADD CONSTRAINT fkhqg7dyfxjble9mpmgygki43o1 FOREIGN KEY (smart_service_id) REFERENCES public.smart_service(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service_practitioner_idp fkjhbxxn4cy6v3n4k7s7k8n8hqg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service_practitioner_idp
    ADD CONSTRAINT fkjhbxxn4cy6v3n4k7s7k8n8hqg FOREIGN KEY (smart_service_id) REFERENCES public.smart_service(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service_related_person_idp fkjw4sngks1cislqtheirhg83u4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service_related_person_idp
    ADD CONSTRAINT fkjw4sngks1cislqtheirhg83u4 FOREIGN KEY (identity_provider_id) REFERENCES public.identity_provider(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: permission fkrvhjnns4bvlh4m1n97vb7vbar; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.permission
    ADD CONSTRAINT fkrvhjnns4bvlh4m1n97vb7vbar FOREIGN KEY (role_id) REFERENCES public.role(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- Name: smart_service_practitioner_idp fks6gnups1834xpewybbufw5i7c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$ BEGIN
    ALTER TABLE ONLY public.smart_service_practitioner_idp
    ADD CONSTRAINT fks6gnups1834xpewybbufw5i7c FOREIGN KEY (identity_provider_id) REFERENCES public.identity_provider(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN duplicate_table THEN NULL;
END $$;


--
-- PostgreSQL database dump complete
--

