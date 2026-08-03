package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderIssueVersionVO;
import com.paper.mes.processorder.entity.ProcessOrderIssueVersion;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** Builds read-only issue-version metadata, including the pre-versioning boundary. */
final class ProcessOrderIssueVersionViewFactory {

    private ProcessOrderIssueVersionViewFactory() {
    }

    static List<ProcessOrderIssueVersionVO> views(String orderUuid,
                                                   List<ProcessOrderIssueVersion> rows,
                                                   boolean hasLegacySnapshot) {
        List<ProcessOrderIssueVersionVO> views = new ArrayList<>(rows.size() + 1);
        rows.stream().map(ProcessOrderIssueVersionViewFactory::toView).forEach(views::add);
        if (hasLegacyBoundary(rows, hasLegacySnapshot)) {
            views.add(legacyView(orderUuid));
        }
        return List.copyOf(views);
    }

    private static boolean hasLegacyBoundary(List<ProcessOrderIssueVersion> rows,
                                             boolean hasLegacySnapshot) {
        if (rows.isEmpty()) {
            return hasLegacySnapshot;
        }
        ProcessOrderIssueVersion earliest = rows.getLast();
        return earliest.getPreviousVersionNo() == null
                && StringUtils.hasText(earliest.getSnapshotBefore());
    }

    private static ProcessOrderIssueVersionVO legacyView(String orderUuid) {
        ProcessOrderIssueVersionVO view = new ProcessOrderIssueVersionVO();
        view.setOrderUuid(orderUuid);
        view.setStatus(ProcessOrderIssueVersionVO.STATUS_LEGACY_UNVERSIONED);
        view.setHasSnapshotAfter(true);
        return view;
    }

    private static ProcessOrderIssueVersionVO toView(ProcessOrderIssueVersion row) {
        ProcessOrderIssueVersionVO view = new ProcessOrderIssueVersionVO();
        view.setUuid(row.getUuid());
        view.setOrderUuid(row.getOrderUuid());
        view.setVersionNo(row.getVersionNo());
        view.setPreviousVersionNo(row.getPreviousVersionNo());
        view.setStatus(row.getStatus());
        view.setChangeReason(row.getChangeReason());
        view.setOperatorName(row.getOperatorName());
        view.setChangeTime(row.getChangeTime());
        view.setIssueTime(row.getIssueTime());
        view.setIssueOperatorName(row.getIssueOperatorName());
        view.setHasSnapshotBefore(StringUtils.hasText(row.getSnapshotBefore()));
        view.setHasSnapshotAfter(StringUtils.hasText(row.getSnapshotAfter()));
        return view;
    }
}
