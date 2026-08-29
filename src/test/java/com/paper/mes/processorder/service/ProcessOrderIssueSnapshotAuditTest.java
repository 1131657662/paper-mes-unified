package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.ProcessOrderIssueVersion;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderIssueSnapshotAuditTest {

    @Test
    void summary_containsVersionSizesAndHashesWithoutSnapshotContent() {
        ProcessOrderIssueVersion version = new ProcessOrderIssueVersion();
        version.setPreviousVersionNo(1);
        version.setVersionNo(2);
        version.setRequestId("request-1");
        version.setSnapshotBefore("old snapshot");

        String result = ProcessOrderIssueSnapshotAudit.summary(version, "new snapshot");

        assertThat(result).contains("下发版本 V1 -> V2", "请求编号 request-1");
        assertThat(result).contains("变更前快照" + "old snapshot".getBytes(StandardCharsets.UTF_8).length + "字节");
        assertThat(result).contains("变更后快照" + "new snapshot".getBytes(StandardCharsets.UTF_8).length + "字节");
        assertThat(result).doesNotContain("old snapshot", "new snapshot");
    }

    @Test
    void summary_forLargeSnapshots_staysWellWithinOperationLogTextCapacity() {
        ProcessOrderIssueVersion version = new ProcessOrderIssueVersion();
        version.setPreviousVersionNo(1);
        version.setVersionNo(2);
        version.setSnapshotBefore("a".repeat(399_000));

        String result = ProcessOrderIssueSnapshotAudit.summary(version, "b".repeat(399_000));

        assertThat(result.getBytes(StandardCharsets.UTF_8)).hasSizeLessThan(1_000);
        assertThat(result).contains("399000字节", "SHA-256=");
    }
}
