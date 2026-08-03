package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderIssueVersionVO;
import com.paper.mes.processorder.entity.ProcessOrderIssueVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderIssueVersionViewFactoryTest {

    @Test
    void noVersionRowsWithIssuedSnapshot_returnsMetadataOnlyLegacyBoundary() {
        ProcessOrderIssueVersionVO legacy = ProcessOrderIssueVersionViewFactory
                .views("order-1", List.of(), true).getFirst();

        assertThat(legacy.getStatus()).isEqualTo(ProcessOrderIssueVersionVO.STATUS_LEGACY_UNVERSIONED);
        assertThat(legacy.getOrderUuid()).isEqualTo("order-1");
        assertThat(legacy.isHasSnapshotAfter()).isTrue();
        assertThat(legacy.getUuid()).isNull();
        assertThat(legacy.getVersionNo()).isNull();
        assertThat(legacy.getOperatorName()).isNull();
        assertThat(legacy.getChangeTime()).isNull();
        assertThat(legacy.getIssueOperatorName()).isNull();
        assertThat(legacy.getIssueTime()).isNull();
    }

    @Test
    void noVersionRowsWithoutIssuedSnapshot_returnsEmptyHistory() {
        assertThat(ProcessOrderIssueVersionViewFactory.views("order-1", List.of(), false))
                .isEmpty();
    }

    @Test
    void initialAppliedVersion_doesNotAddLegacyBoundary() {
        ProcessOrderIssueVersion initial = version(1, null, "issued-json");

        List<ProcessOrderIssueVersionVO> views = ProcessOrderIssueVersionViewFactory
                .views("order-1", List.of(initial), true);

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.getVersionNo()).isEqualTo(1);
            assertThat(view.getStatus()).isEqualTo(ProcessOrderIssueVersion.STATUS_APPLIED);
        });
    }

    @Test
    void firstReissueFromLegacySnapshot_retainsLegacyBoundaryAfterRealVersion() {
        ProcessOrderIssueVersion reissue = version(1, "legacy-json", "reissued-json");

        List<ProcessOrderIssueVersionVO> views = ProcessOrderIssueVersionViewFactory
                .views("order-1", List.of(reissue), true);

        assertThat(views).extracting(ProcessOrderIssueVersionVO::getStatus)
                .containsExactly(ProcessOrderIssueVersion.STATUS_APPLIED,
                        ProcessOrderIssueVersionVO.STATUS_LEGACY_UNVERSIONED);
    }

    private ProcessOrderIssueVersion version(int versionNo, String snapshotBefore, String snapshotAfter) {
        ProcessOrderIssueVersion row = new ProcessOrderIssueVersion();
        row.setUuid("version-" + versionNo);
        row.setOrderUuid("order-1");
        row.setVersionNo(versionNo);
        row.setSnapshotBefore(snapshotBefore);
        row.setSnapshotAfter(snapshotAfter);
        row.setStatus(ProcessOrderIssueVersion.STATUS_APPLIED);
        return row;
    }
}
