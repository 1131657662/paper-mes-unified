package com.paper.mes.exporttask.service;

import com.paper.mes.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExportTaskHandlerRegistry {
    private final Map<String, ExportTaskHandler> handlers;

    public ExportTaskHandlerRegistry(List<ExportTaskHandler> handlerList) {
        handlers = handlerList.stream().collect(Collectors.toUnmodifiableMap(
                ExportTaskHandler::taskType,
                Function.identity(),
                (left, right) -> {
                    throw new IllegalStateException("重复的导出任务类型：" + left.taskType());
                }));
    }

    public ExportTaskHandler require(String taskType) {
        ExportTaskHandler handler = handlers.get(taskType);
        if (handler == null) {
            throw new BusinessException("不支持的导出任务类型：" + taskType);
        }
        return handler;
    }

    public Optional<ExportTaskHandler> find(String taskType) {
        return Optional.ofNullable(handlers.get(taskType));
    }
}
