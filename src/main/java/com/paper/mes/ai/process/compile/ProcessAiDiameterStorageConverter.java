package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiMeasurement;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
class ProcessAiDiameterStorageConverter {

    private static final BigDecimal INCH_TO_MM = new BigDecimal("25.4");

    int targetDiameter(ProcessAiMeasurement measurement, int defaultMm) {
        if (measurement == null) return defaultMm;
        if ("inch".equals(measurement.unit())) return exactPositiveInt(measurement.value());
        BigDecimal mm = measurement.value();
        if (mm.compareTo(new BigDecimal("100")) >= 0) return exactPositiveInt(mm);
        return exactInches(mm, "目标直径小于100mm时必须能精确转换为整数英寸");
    }

    int coreDiameter(ProcessAiMeasurement measurement) {
        if (measurement == null) return 3;
        if ("inch".equals(measurement.unit())) return exactPositiveInt(measurement.value());
        BigDecimal mm = measurement.value();
        if (mm.compareTo(BigDecimal.TEN) >= 0) return exactPositiveInt(mm);
        return exactInches(mm, "纸芯小于10mm时无法按现有字段安全存储");
    }

    private int exactInches(BigDecimal mm, String message) {
        BigDecimal inches = mm.divide(INCH_TO_MM, MathContext.DECIMAL128);
        try {
            return exactPositiveInt(inches);
        } catch (BusinessException ex) {
            throw invalid(message);
        }
    }

    private int exactPositiveInt(BigDecimal value) {
        try {
            int result = value.stripTrailingZeros().intValueExact();
            if (result <= 0) throw invalid("直径和纸芯必须大于0");
            return result;
        } catch (ArithmeticException ex) {
            throw invalid("现有整数直径字段无法无损保存该值");
        }
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST,
                "AI_DIAMETER_STORAGE_UNSAFE", message);
    }
}
