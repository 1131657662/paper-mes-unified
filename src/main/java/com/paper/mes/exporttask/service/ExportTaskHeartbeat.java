package com.paper.mes.exporttask.service;

@FunctionalInterface
public interface ExportTaskHeartbeat extends AutoCloseable {
    @Override
    void close();
}
