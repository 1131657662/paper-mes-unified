package com.paper.mes.exporttask.dto;

import com.paper.mes.exporttask.entity.ExportTask;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTaskVOTest {
    @Test
    void from_includesRetryBudget() {
        ExportTask task = new ExportTask();
        task.setAttemptCount(2);
        task.setMaxAttempts(3);

        ExportTaskVO result = ExportTaskVO.from(task, false);

        assertThat(result.attemptCount()).isEqualTo(2);
        assertThat(result.maxAttempts()).isEqualTo(3);
        assertThat(result.resourceAccessible()).isFalse();
    }
}
