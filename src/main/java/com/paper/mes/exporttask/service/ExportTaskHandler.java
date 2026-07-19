package com.paper.mes.exporttask.service;

import com.paper.mes.exporttask.entity.ExportTask;

import java.nio.file.Path;

public interface ExportTaskHandler {

    String taskType();

    String requiredPermission();

    String fileExtension();

    ExportTaskArtifact generate(ExportTask task, Path target) throws Exception;
}
