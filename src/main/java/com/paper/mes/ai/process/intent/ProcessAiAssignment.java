package com.paper.mes.ai.process.intent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProcessAiAssignment(
        @NotNull @Size(min = 1, max = 100) List<@Pattern(regexp = "R[1-9]\\d{0,2}") String> sourceRollRefs,
        @NotBlank @Pattern(regexp = "R[1-9]\\d{0,2}") String ownerRollRef,
        @NotNull @Size(max = 99) List<@Pattern(regexp = "R[1-9]\\d{0,2}") String> coveredRollRefs,
        @NotBlank @Pattern(regexp = "REWIND|SAW|DIRECT_SHIP|SERVICE_ONLY|ANCILLARY_ONLY") String processType,
        @Pattern(regexp = "STANDARD|ON_SITE|DIRECT_SHIP|SERVICE_ONLY") String processMode,
        @Valid ProcessAiRewindIntent rewindIntent,
        @Valid ProcessAiSawIntent sawIntent,
        @Valid ProcessAiAncillaryRequirements ancillaryRequirements,
        @NotNull @Size(min = 1, max = 30) List<@Valid ProcessAiEvidence> evidence,
        @NotNull @Size(max = 500) List<@Valid ProcessAiCustomerSpec> customerSpecs) {

    public ProcessAiAssignment(List<String> sourceRollRefs, String ownerRollRef,
                               List<String> coveredRollRefs, String processType,
                               ProcessAiRewindIntent rewindIntent, ProcessAiSawIntent sawIntent,
                               ProcessAiAncillaryRequirements ancillaryRequirements,
                               List<ProcessAiEvidence> evidence) {
        this(sourceRollRefs, ownerRollRef, coveredRollRefs, processType, null, rewindIntent, sawIntent,
                ancillaryRequirements, evidence, List.of());
    }

    /** Compatibility constructor for persisted schema-1.0 results without processMode. */
    public ProcessAiAssignment(List<String> sourceRollRefs, String ownerRollRef,
                               List<String> coveredRollRefs, String processType,
                               ProcessAiRewindIntent rewindIntent, ProcessAiSawIntent sawIntent,
                               ProcessAiAncillaryRequirements ancillaryRequirements,
                               List<ProcessAiEvidence> evidence,
                               List<ProcessAiCustomerSpec> customerSpecs) {
        this(sourceRollRefs, ownerRollRef, coveredRollRefs, processType, null, rewindIntent, sawIntent,
                ancillaryRequirements, evidence, customerSpecs);
    }

    public ProcessAiAssignment {
        sourceRollRefs = sourceRollRefs == null ? null : List.copyOf(sourceRollRefs);
        coveredRollRefs = coveredRollRefs == null ? null : List.copyOf(coveredRollRefs);
        evidence = evidence == null ? null : List.copyOf(evidence);
        customerSpecs = customerSpecs == null ? List.of() : List.copyOf(customerSpecs);
    }
}
