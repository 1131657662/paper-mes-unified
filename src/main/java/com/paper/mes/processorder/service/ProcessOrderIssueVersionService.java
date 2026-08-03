package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.ProcessOrderIssueVersionVO;
import com.paper.mes.processorder.entity.ProcessOrderIssueVersion;
import com.paper.mes.processorder.mapper.ProcessOrderIssueVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Owns version numbering and retained before/after issue snapshots. */
@Component
@RequiredArgsConstructor
public class ProcessOrderIssueVersionService {

    private final ProcessOrderIssueVersionMapper mapper;

    public Optional<ProcessOrderIssueVersion> findPending(String orderUuid) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<ProcessOrderIssueVersion>()
                .eq(ProcessOrderIssueVersion::getOrderUuid, orderUuid)
                .eq(ProcessOrderIssueVersion::getStatus, ProcessOrderIssueVersion.STATUS_PENDING)
                .orderByDesc(ProcessOrderIssueVersion::getVersionNo)
                .last("LIMIT 1")));
    }

    public int nextVersion(String orderUuid) {
        ProcessOrderIssueVersion latest = mapper.selectOne(new LambdaQueryWrapper<ProcessOrderIssueVersion>()
                .eq(ProcessOrderIssueVersion::getOrderUuid, orderUuid)
                .orderByDesc(ProcessOrderIssueVersion::getVersionNo)
                .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    public ProcessOrderIssueVersion recordInitial(String orderUuid, String snapshot,
                                                  String operator, LocalDateTime issueTime) {
        requireSnapshot(snapshot);
        ProcessOrderIssueVersion row = newRow(orderUuid, nextVersion(orderUuid), operator, issueTime);
        row.setSnapshotAfter(snapshot);
        row.setIssueTime(issueTime);
        row.setIssueOperatorName(operator);
        row.setStatus(ProcessOrderIssueVersion.STATUS_APPLIED);
        insert(row);
        return row;
    }

    public ProcessOrderIssueVersion prepare(String orderUuid, String snapshotBefore,
                                             String reason, String operator, LocalDateTime changeTime) {
        requireSnapshot(snapshotBefore);
        if (findPending(orderUuid).isPresent()) {
            throw new BusinessException(ErrorCode.E003, "该加工单已有待应用的重新下发变更");
        }
        int previousVersion = nextVersion(orderUuid) - 1;
        ProcessOrderIssueVersion row = newRow(orderUuid, previousVersion + 1, operator, changeTime);
        row.setPreviousVersionNo(previousVersion > 0 ? previousVersion : null);
        row.setSnapshotBefore(snapshotBefore);
        row.setChangeReason(reason.trim());
        row.setStatus(ProcessOrderIssueVersion.STATUS_PENDING);
        insert(row);
        return row;
    }

    public void apply(ProcessOrderIssueVersion row, String snapshotAfter,
                      String operator, LocalDateTime issueTime) {
        requireSnapshot(snapshotAfter);
        row.setSnapshotAfter(snapshotAfter);
        row.setIssueTime(issueTime);
        row.setIssueOperatorName(operator);
        row.setStatus(ProcessOrderIssueVersion.STATUS_APPLIED);
        ConcurrencyGuard.requireRowUpdated(mapper.updateById(row));
    }

    public void cancelPending(String orderUuid, String reason, String operator, LocalDateTime changeTime) {
        ProcessOrderIssueVersion row = findPending(orderUuid).orElse(null);
        if (row == null) {
            return;
        }
        row.setStatus(ProcessOrderIssueVersion.STATUS_ARCHIVED);
        row.setChangeReason(row.getChangeReason() + "；变更取消：" + reason.trim());
        row.setOperatorName(StringUtils.hasText(operator) ? operator : "system");
        row.setChangeTime(changeTime);
        ConcurrencyGuard.requireRowUpdated(mapper.updateById(row));
    }

    public void archive(String orderUuid, String snapshotBefore, String reason,
                        String operator, LocalDateTime changeTime) {
        if (!StringUtils.hasText(snapshotBefore) || findPending(orderUuid).isPresent()) {
            return;
        }
        ProcessOrderIssueVersion latest = latest(orderUuid);
        if (latest != null && (StringUtils.hasText(latest.getSnapshotAfter())
                || (ProcessOrderIssueVersion.STATUS_ARCHIVED.equals(latest.getStatus())
                && Objects.equals(latest.getSnapshotBefore(), snapshotBefore)))) {
            return;
        }
        int previousVersion = nextVersion(orderUuid) - 1;
        ProcessOrderIssueVersion row = newRow(orderUuid, previousVersion + 1, operator, changeTime);
        row.setPreviousVersionNo(previousVersion > 0 ? previousVersion : null);
        row.setSnapshotBefore(snapshotBefore);
        row.setChangeReason(StringUtils.hasText(reason) ? reason.trim() : "历史下发快照归档");
        row.setStatus(ProcessOrderIssueVersion.STATUS_ARCHIVED);
        insert(row);
    }

    public List<ProcessOrderIssueVersionVO> list(String orderUuid) {
        return mapper.selectList(new LambdaQueryWrapper<ProcessOrderIssueVersion>()
                        .eq(ProcessOrderIssueVersion::getOrderUuid, orderUuid)
                        .orderByDesc(ProcessOrderIssueVersion::getVersionNo))
                .stream().map(this::toView).toList();
    }

    private ProcessOrderIssueVersion newRow(String orderUuid, int versionNo,
                                            String operator, LocalDateTime changeTime) {
        ProcessOrderIssueVersion row = new ProcessOrderIssueVersion();
        row.setOrderUuid(orderUuid);
        row.setVersionNo(versionNo);
        row.setOperatorName(StringUtils.hasText(operator) ? operator : "system");
        row.setChangeTime(changeTime);
        return row;
    }

    private ProcessOrderIssueVersion latest(String orderUuid) {
        return mapper.selectOne(new LambdaQueryWrapper<ProcessOrderIssueVersion>()
                .eq(ProcessOrderIssueVersion::getOrderUuid, orderUuid)
                .orderByDesc(ProcessOrderIssueVersion::getVersionNo)
                .last("LIMIT 1"));
    }

    private void insert(ProcessOrderIssueVersion row) {
        try {
            ConcurrencyGuard.requireRowUpdated(mapper.insert(row));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.E006, "加工单版本已被其他操作生成，请刷新后重试");
        }
    }

    private void requireSnapshot(String snapshot) {
        if (!StringUtils.hasText(snapshot)) {
            throw new BusinessException(ErrorCode.E002, "下发快照不存在，无法生成版本历史");
        }
    }

    private ProcessOrderIssueVersionVO toView(ProcessOrderIssueVersion row) {
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
