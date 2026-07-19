package com.paper.mes.exporttask.service;

import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportTaskHandlerRegistryTest {

    @Test
    void require_forRegisteredType_returnsHandler() {
        ExportTaskHandler handler = handler("REPORT_EXPORT");
        ExportTaskHandlerRegistry registry = new ExportTaskHandlerRegistry(List.of(handler));

        assertThat(registry.require("REPORT_EXPORT")).isSameAs(handler);
    }

    @Test
    void require_forUnknownType_rejectsTask() {
        ExportTaskHandlerRegistry registry = new ExportTaskHandlerRegistry(List.of(handler("SETTLE_DETAIL")));

        assertThatThrownBy(() -> registry.require("UNKNOWN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的导出任务类型");
    }

    @Test
    void find_forUnknownType_returnsEmptyResult() {
        ExportTaskHandlerRegistry registry = new ExportTaskHandlerRegistry(List.of(handler("SETTLE_DETAIL")));

        assertThat(registry.find("UNKNOWN")).isEmpty();
    }

    private ExportTaskHandler handler(String type) {
        ExportTaskHandler handler = mock(ExportTaskHandler.class);
        when(handler.taskType()).thenReturn(type);
        return handler;
    }
}
