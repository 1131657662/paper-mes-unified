package com.paper.mes.report.service;

import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQueryExecutionMetaVO;
import com.paper.mes.report.dto.ReportQuerySnapshotVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class ReportQuerySnapshotService {
    private static final long SNAPSHOT_TTL_MINUTES = 30;
    private static final long EXPORT_SNAPSHOT_TTL_DAYS = 8;
    private static final long IDEMPOTENCY_WINDOW_SECONDS = 10;

    private final ReportQueryCoordinator coordinator;
    private final ReportQuerySnapshotStore store;
    private final ReportQueryScopePolicy scopePolicy;
    private final Clock clock;

    @Autowired
    public ReportQuerySnapshotService(ReportQueryCoordinator coordinator,
                                      ReportQuerySnapshotStore store,
                                      ReportQueryScopePolicy scopePolicy) {
        this(coordinator, store, scopePolicy, Clock.systemDefaultZone());
    }

    ReportQuerySnapshotService(ReportQueryCoordinator coordinator,
                               ReportQuerySnapshotStore store,
                               ReportQueryScopePolicy scopePolicy,
                               Clock clock) {
        this.coordinator = coordinator;
        this.store = store;
        this.scopePolicy = scopePolicy;
        this.clock = clock;
    }

    public ReportQuerySnapshotVO create(ReportQuery query) {
        CurrentUser user = scopePolicy.currentUser();
        return createBundle(query, user, SNAPSHOT_TTL_MINUTES, ChronoUnit.MINUTES).snapshot();
    }

    public ReportQuerySnapshotBundle createForExport(ReportQuery query, CurrentUser owner) {
        if (owner == null || owner.getUuid() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效");
        }
        return createBundle(query, owner, EXPORT_SNAPSHOT_TTL_DAYS, ChronoUnit.DAYS);
    }

    private ReportQuerySnapshotBundle createBundle(ReportQuery query, CurrentUser user,
                                                   long ttl, ChronoUnit unit) {
        ReportQueryExecutionMetaVO execution = coordinator.prepare(query);
        String permissionHash = scopePolicy.permissionHash(user);
        ReportQuerySnapshotLookup lookup = lookup(user, permissionHash, execution);
        var reusable = store.findReusable(lookup);
        if (reusable.isPresent()) return bundle(reusable.get(), execution.metricReleaseUuid());
        ReportQuerySnapshotVO snapshot = snapshot(
                execution, scopePolicy.scopeHash(user, permissionHash), ttl, unit);
        ReportQuery lockedQuery = lockedQuery(query, execution.metricReleaseUuid());
        try {
            store.insert(new ReportQuerySnapshotRecord(user.getUuid(), permissionHash, lockedQuery, snapshot),
                    user.getRoleCode(), lookup.idempotencyBucket());
            return new ReportQuerySnapshotBundle(lockedQuery, snapshot);
        } catch (DuplicateKeyException conflict) {
            return store.findReusable(lookup)
                    .map(record -> bundle(record, execution.metricReleaseUuid()))
                    .orElseThrow(() -> new BusinessException(ResultCode.CONFLICT,
                            "REPORT_QUERY_SNAPSHOT_CONFLICT", "报表查询正在生成，请稍后重试"));
        }
    }

    private ReportQuerySnapshotLookup lookup(CurrentUser user, String permissionHash,
                                              ReportQueryExecutionMetaVO execution) {
        return new ReportQuerySnapshotLookup(user.getUuid(), permissionHash, execution.queryHash(),
                execution.metricReleaseUuid(), Math.floorDiv(clock.instant().getEpochSecond(),
                        IDEMPOTENCY_WINDOW_SECONDS));
    }

    public ReportQuerySnapshotVO requireAccessible(String uuid) {
        CurrentUser user = scopePolicy.currentUser();
        ReportQuerySnapshotRecord record = store.find(uuid).orElseThrow(this::notFound);
        if (!user.getUuid().equals(record.ownerUuid())) throw notFound();
        if (isExpired(record.snapshot())) throw notFound();
        String currentHash = scopePolicy.permissionHash(user);
        if (!currentHash.equals(record.permissionHash())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "REPORT_SCOPE_FORBIDDEN",
                    "当前权限范围已变化，请重新查询");
        }
        return record.snapshot();
    }

    private ReportQuerySnapshotVO snapshot(ReportQueryExecutionMetaVO execution, String scopeHash,
                                           long ttl, ChronoUnit unit) {
        LocalDateTime expiresAt = LocalDateTime.now(clock).plus(ttl, unit);
        return new ReportQuerySnapshotVO(execution.queryId(), execution.queryId(), execution.queryHash(),
                execution.metricReleaseUuid(), execution.metricVersionMap(), execution.dataAsOf(),
                execution.sourceWatermark(), expiresAt, scopeHash, execution.consistencyMode(),
                execution.coverage(), execution.warnings(), execution.sectionStatuses());
    }

    private ReportQuerySnapshotBundle bundle(ReportQuerySnapshotRecord record, String releaseUuid) {
        return new ReportQuerySnapshotBundle(lockedQuery(record.query(), releaseUuid), record.snapshot());
    }

    private ReportQuery lockedQuery(ReportQuery source, String releaseUuid) {
        ReportQuery target = new ReportQuery();
        if (source != null) copyFilters(source, target);
        target.setMetricReleaseUuid(releaseUuid);
        return target;
    }

    private void copyFilters(ReportQuery source, ReportQuery target) {
        target.setDateFrom(source.getDateFrom());
        target.setDateTo(source.getDateTo());
        target.setCustomerUuid(source.getCustomerUuid());
        target.setPaperName(source.getPaperName());
        target.setMainStepType(source.getMainStepType());
        target.setProcessStepType(source.getProcessStepType());
        target.setProcessMode(source.getProcessMode());
        target.setMachineUuid(source.getMachineUuid());
        target.setSettleType(source.getSettleType());
        target.setIsInvoice(source.getIsInvoice());
        target.setOrderStatus(source.getOrderStatus());
        target.setDimension(source.getDimension());
    }

    private boolean isExpired(ReportQuerySnapshotVO snapshot) {
        return !snapshot.expiresAt().isAfter(LocalDateTime.now(clock));
    }

    private BusinessException notFound() {
        return new BusinessException(ResultCode.NOT_FOUND, "QUERY_SNAPSHOT_NOT_FOUND",
                "查询快照不存在或已过期");
    }
}
