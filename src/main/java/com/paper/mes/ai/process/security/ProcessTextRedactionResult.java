package com.paper.mes.ai.process.security;

import java.util.List;

public record ProcessTextRedactionResult(
        String sanitizedText,
        List<ProcessTextRedactor.ExtractedCharge> charges,
        boolean modified) {

    public ProcessTextRedactionResult {
        charges = List.copyOf(charges);
    }
}
