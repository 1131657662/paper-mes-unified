package com.paper.mes.exporttask.service;

import com.paper.mes.exporttask.entity.ExportTask;

public record ExportTaskExecutionLease(ExportTask task, String token) {
}
