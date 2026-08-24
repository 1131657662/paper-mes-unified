package com.paper.mes.ai.process.parse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Typed correction value; arbitrary JSON pointers and object values are intentionally absent. */
public record ProcessAiCorrection(
        @NotBlank @Pattern(regexp = "R[1-9]\\d{0,2}") String assignmentRef,
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z][A-Za-z0-9_.:-]{0,63}") String field,
        BigDecimal value,
        @Size(max = 64) @Pattern(regexp = "[^\\p{Cntrl}]*") String textValue,
        @Size(max = 16) @Pattern(regexp = "[A-Za-z]{1,16}") String unit,
        @Min(0) Integer outputIndex) {

    public ProcessAiCorrection(String assignmentRef, String field, BigDecimal value,
                               String textValue, String unit) {
        this(assignmentRef, field, value, textValue, unit, null);
    }

    public ProcessAiCorrection(String assignmentRef, String field, int value, String unit) {
        this(assignmentRef, field, BigDecimal.valueOf(value), null, unit, null);
    }
}
