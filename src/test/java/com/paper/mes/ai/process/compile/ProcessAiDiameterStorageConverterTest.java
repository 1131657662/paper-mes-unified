package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiMeasurement;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessAiDiameterStorageConverterTest {

    private final ProcessAiDiameterStorageConverter converter =
            new ProcessAiDiameterStorageConverter();

    @Test
    void targetDiameter_whenSmallMmIsExactInches_storesInches() {
        int stored = converter.targetDiameter(measurement("76.2", "mm"), 1200);

        assertEquals(3, stored);
    }

    @Test
    void targetDiameter_whenSmallMmIsAmbiguous_rejectsCompilation() {
        assertThrows(BusinessException.class,
                () -> converter.targetDiameter(measurement("76", "mm"), 1200));
    }

    @Test
    void coreDiameter_whenMmValueIsSafe_storesMillimeters() {
        int stored = converter.coreDiameter(measurement("76", "mm"));

        assertEquals(76, stored);
    }

    @Test
    void targetDiameter_whenMissing_usesConfiguredDefault() {
        assertEquals(1200, converter.targetDiameter(null, 1200));
    }

    private ProcessAiMeasurement measurement(String value, String unit) {
        return new ProcessAiMeasurement(new BigDecimal(value), unit, "EXPLICIT");
    }
}
