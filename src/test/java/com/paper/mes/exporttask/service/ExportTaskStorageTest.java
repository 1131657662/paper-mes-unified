package com.paper.mes.exporttask.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.exporttask.entity.ExportTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportTaskStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void requireFile_withPathOutsideStorageRoot_rejectsDownload() throws Exception {
        Path outside = Files.writeString(tempDir.resolve("outside.xlsx"), "data");
        ExportTask task = new ExportTask();
        task.setFilePath(outside.toString());
        ExportTaskStorage storage = new ExportTaskStorage(tempDir.resolve("exports").toString());

        assertThatThrownBy(() -> storage.requireFile(task)).isInstanceOf(BusinessException.class);
    }

    @Test
    void requireFile_withGeneratedTaskPath_returnsRegularFile() throws Exception {
        ExportTaskStorage storage = new ExportTaskStorage(tempDir.resolve("exports").toString());
        Path target = storage.target(lease("lease-1"), "xlsx");
        Files.writeString(target, "data");
        ExportTask task = new ExportTask();
        task.setFilePath(target.toString());

        assertThat(storage.requireFile(task)).isEqualTo(target);
    }

    @Test
    void requireFile_withRelativeStorageKey_resolvesFromConfiguredRoot() throws Exception {
        ExportTaskStorage storage = new ExportTaskStorage(tempDir.resolve("exports").toString());
        Path target = storage.target(lease("lease-relative"), "xlsx");
        Files.writeString(target, "data");
        ExportTask task = new ExportTask();
        task.setFilePath(storage.storageKey(target));

        assertThat(storage.requireFile(task)).isEqualTo(target);
    }

    @Test
    void requireFile_withLegacyAbsoluteExecutionPath_resolvesOnNewInstanceRoot() throws Exception {
        Path oldRoot = tempDir.resolve("old-exports");
        Path newRoot = tempDir.resolve("shared-exports");
        ExportTaskStorage oldStorage = new ExportTaskStorage(oldRoot.toString());
        ExportTaskStorage newStorage = new ExportTaskStorage(newRoot.toString());
        Path oldTarget = oldStorage.target(lease("lease-legacy"), "xlsx");
        Path newTarget = newRoot.resolve(oldTarget.getFileName());
        Files.createDirectories(newRoot);
        Files.writeString(newTarget, "data");
        ExportTask task = new ExportTask();
        task.setFilePath(oldTarget.toString());

        assertThat(newStorage.requireFile(task)).isEqualTo(newTarget);
    }

    @Test
    void target_withDifferentExecutionLeases_isolatesArtifacts() throws Exception {
        ExportTaskStorage storage = new ExportTaskStorage(tempDir.resolve("exports").toString());
        Path staleTarget = storage.target(lease("lease-1"), "xlsx");
        Path currentTarget = storage.target(lease("lease-2"), "xlsx");
        Files.writeString(staleTarget, "stale");
        Files.writeString(currentTarget, "current");

        storage.delete(staleTarget);

        assertThat(currentTarget).isNotEqualTo(staleTarget);
        assertThat(Files.readString(currentTarget)).isEqualTo("current");
    }

    @Test
    void target_withPathTraversalToken_rejectsArtifactPath() {
        ExportTaskStorage storage = new ExportTaskStorage(tempDir.resolve("exports").toString());

        assertThatThrownBy(() -> storage.target(lease("../lease"), "xlsx"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void health_withAvailableRoot_reportsFilesystemCapacity() {
        ExportTaskStorage storage = new ExportTaskStorage(tempDir.resolve("exports").toString());

        ExportTaskStorageHealth health = storage.health(0, 0);

        assertThat(health.status()).isEqualTo(ExportTaskStorageHealth.READY);
        assertThat(health.available()).isTrue();
        assertThat(health.writable()).isTrue();
        assertThat(health.totalBytes()).isPositive();
    }

    @Test
    void health_withImpossibleFreeSpaceThreshold_reportsLowSpace() {
        ExportTaskStorage storage = new ExportTaskStorage(tempDir.resolve("exports").toString());

        ExportTaskStorageHealth health = storage.health(Long.MAX_VALUE, 0);

        assertThat(health.status()).isEqualTo(ExportTaskStorageHealth.LOW_SPACE);
        assertThat(health.available()).isFalse();
        assertThat(health.writable()).isTrue();
    }

    private ExportTaskExecutionLease lease(String token) {
        ExportTask task = new ExportTask();
        task.setUuid("task-1");
        return new ExportTaskExecutionLease(task, token);
    }
}
