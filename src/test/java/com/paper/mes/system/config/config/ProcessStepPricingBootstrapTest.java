package com.paper.mes.system.config.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessStepPricingBootstrapTest {

    @Test
    void bootstrap_whenAddingPricingSchema_isIdempotentAndConstrained() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/paper/mes/system/config/config/ProcessStepPricingBootstrap.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("columnExists(name)"));
        assertTrue(source.contains("constraintExists(name)"));
        assertTrue(source.contains("chk_process_step_billing_mode"));
        assertTrue(source.contains("chk_process_step_pricing_nonnegative"));
        assertTrue(source.contains("billing_quantity"));
        assertTrue(source.contains("pricing_adjustment_reason"));
        assertTrue(source.contains("billing_unit_price"));
        assertTrue(source.contains("pricing_adjustment_batch_id"));
    }
}
