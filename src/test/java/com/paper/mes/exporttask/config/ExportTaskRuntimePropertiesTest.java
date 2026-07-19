package com.paper.mes.exporttask.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTaskRuntimePropertiesTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void defaults_keepHeartbeatSafelyInsideStaleWindow() {
        assertThat(validator.validate(new ExportTaskRuntimeProperties())).isEmpty();
    }

    @Test
    void heartbeatBeyondOneThirdOfStaleWindow_isRejected() {
        ExportTaskRuntimeProperties properties = new ExportTaskRuntimeProperties();
        properties.setStaleMinutes(1);
        properties.setHeartbeatIntervalSeconds(30);

        assertThat(validator.validate(properties)).singleElement()
                .extracting("message")
                .isEqualTo("导出任务心跳间隔必须不超过停滞窗口的三分之一");
    }

    @Test
    void orphanRetentionShorterThanStaleWindow_isRejected() {
        ExportTaskRuntimeProperties properties = new ExportTaskRuntimeProperties();
        properties.setOrphanRetentionMinutes(5);

        assertThat(validator.validate(properties)).singleElement()
                .extracting("message")
                .isEqualTo("导出孤儿文件保留窗口不能小于停滞恢复窗口");
    }
}
