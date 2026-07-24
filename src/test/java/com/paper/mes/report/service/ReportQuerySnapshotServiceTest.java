package com.paper.mes.report.service;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQueryExecutionMetaVO;
import com.paper.mes.report.dto.ReportQuerySnapshotVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ReportQuerySnapshotServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-21T02:00:00Z"), ZoneOffset.UTC);

    @AfterEach
    void clearAuth() {
        AuthContextHolder.clear();
    }

    @Test
    void create_withAuthenticatedUser_persistsOwnedSnapshot() {
        Fixture fixture = fixture();
        AuthContextHolder.setCurrentUser(user("viewer-1", "viewer"));

        ReportQuerySnapshotVO result = fixture.service().create(new ReportQuery());

        assertEquals("query-1", result.querySnapshotUuid());
        assertEquals(LocalDateTime.of(2026, 7, 21, 2, 30), result.expiresAt());
        verify(fixture.store()).insert(any(ReportQuerySnapshotRecord.class), eq("viewer"), anyLong());
    }

    @Test
    void create_withProcessDrillFilter_locksFilterIntoSnapshot() {
        Fixture fixture = fixture();
        AuthContextHolder.setCurrentUser(user("viewer-1", "viewer"));
        ReportQuery query = new ReportQuery();
        query.setProcessStepType(3);

        fixture.service().create(query);

        ArgumentCaptor<ReportQuerySnapshotRecord> record = ArgumentCaptor.forClass(ReportQuerySnapshotRecord.class);
        verify(fixture.store()).insert(record.capture(), eq("viewer"), anyLong());
        assertEquals(3, record.getValue().query().getProcessStepType());
    }

    @Test
    void create_whenEquivalentSnapshotWasJustCreated_reusesIt() {
        Fixture fixture = fixture();
        AuthContextHolder.setCurrentUser(user("viewer-1", "viewer"));
        ReportQuerySnapshotRecord reusable = record("viewer-1", fixture.scopePolicy(), futureSnapshot());
        when(fixture.store().findReusable(any())).thenReturn(Optional.of(reusable));

        ReportQuerySnapshotVO result = fixture.service().create(new ReportQuery());

        assertEquals("snapshot-1", result.querySnapshotUuid());
        verify(fixture.store(), org.mockito.Mockito.never())
                .insert(any(ReportQuerySnapshotRecord.class), any(), anyLong());
    }

    @Test
    void create_whenConcurrentInsertWins_reusesCommittedSnapshot() {
        Fixture fixture = fixture();
        AuthContextHolder.setCurrentUser(user("viewer-1", "viewer"));
        ReportQuerySnapshotRecord committed = record("viewer-1", fixture.scopePolicy(), futureSnapshot());
        when(fixture.store().findReusable(any()))
                .thenReturn(Optional.empty(), Optional.of(committed));
        doThrow(new DuplicateKeyException("concurrent snapshot"))
                .when(fixture.store()).insert(any(), any(), anyLong());

        ReportQuerySnapshotVO result = fixture.service().create(new ReportQuery());

        assertEquals("snapshot-1", result.querySnapshotUuid());
    }

    @Test
    void requireAccessible_whenOwnedByAnotherUser_returnsNotFound() {
        Fixture fixture = fixture();
        AuthContextHolder.setCurrentUser(user("viewer-1", "viewer"));
        when(fixture.store().find("snapshot-1"))
                .thenReturn(Optional.of(record("viewer-2", fixture.scopePolicy(), futureSnapshot())));

        BusinessException error = assertThrows(BusinessException.class,
                () -> fixture.service().requireAccessible("snapshot-1"));

        assertEquals(ResultCode.NOT_FOUND, error.getCode());
        assertEquals("QUERY_SNAPSHOT_NOT_FOUND", error.getErrorCode());
    }

    @Test
    void requireAccessible_whenPermissionsChanged_returnsForbidden() {
        Fixture fixture = fixture();
        CurrentUser original = user("viewer-1", "viewer");
        AuthContextHolder.setCurrentUser(original);
        String oldHash = fixture.scopePolicy().permissionHash(original);
        when(fixture.store().find("snapshot-1")).thenReturn(Optional.of(
                new ReportQuerySnapshotRecord("viewer-1", oldHash, new ReportQuery(), futureSnapshot())));
        AuthContextHolder.setCurrentUser(user("viewer-1", "admin"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> fixture.service().requireAccessible("snapshot-1"));

        assertEquals(ResultCode.FORBIDDEN, error.getCode());
        assertEquals("REPORT_SCOPE_FORBIDDEN", error.getErrorCode());
    }

    @Test
    void requireAccessible_whenExpired_returnsNotFound() {
        Fixture fixture = fixture();
        CurrentUser user = user("viewer-1", "viewer");
        AuthContextHolder.setCurrentUser(user);
        when(fixture.store().find("snapshot-1")).thenReturn(Optional.of(
                record("viewer-1", fixture.scopePolicy(), expiredSnapshot())));

        BusinessException error = assertThrows(BusinessException.class,
                () -> fixture.service().requireAccessible("snapshot-1"));

        assertEquals(ResultCode.NOT_FOUND, error.getCode());
    }

    private Fixture fixture() {
        ReportQueryCoordinator coordinator = mock(ReportQueryCoordinator.class);
        ReportQuerySnapshotStore store = mock(ReportQuerySnapshotStore.class);
        ReportQueryScopePolicy policy = new ReportQueryScopePolicy();
        when(coordinator.prepare(any())).thenReturn(execution());
        return new Fixture(new ReportQuerySnapshotService(coordinator, store, policy, CLOCK), store, policy);
    }

    private ReportQuerySnapshotRecord record(
            String ownerUuid, ReportQueryScopePolicy policy, ReportQuerySnapshotVO snapshot) {
        return new ReportQuerySnapshotRecord(ownerUuid, policy.permissionHash(user(ownerUuid, "viewer")),
                new ReportQuery(), snapshot);
    }

    private ReportQueryExecutionMetaVO execution() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 2, 0);
        return new ReportQueryExecutionMetaVO("query-1", "hash", "release-1",
                Map.of("order_count", "version-1"), now, now, "LIVE_DB_READ", "LIVE_ONLY",
                List.of(), Map.of("overview", "READY"));
    }

    private ReportQuerySnapshotVO futureSnapshot() {
        return snapshot(LocalDateTime.of(2026, 7, 21, 2, 30));
    }

    private ReportQuerySnapshotVO expiredSnapshot() {
        return snapshot(LocalDateTime.of(2026, 7, 21, 1, 59));
    }

    private ReportQuerySnapshotVO snapshot(LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 2, 0);
        return new ReportQuerySnapshotVO("snapshot-1", "snapshot-1", "hash", "release-1",
                Map.of(), now, now, expiresAt, "scope", "LIVE_DB_READ", "LIVE_ONLY",
                List.of(), Map.of());
    }

    private CurrentUser user(String uuid, String roleCode) {
        return CurrentUser.builder().uuid(uuid).username(uuid).realName(uuid).roleCode(roleCode).build();
    }

    private record Fixture(
            ReportQuerySnapshotService service,
            ReportQuerySnapshotStore store,
            ReportQueryScopePolicy scopePolicy
    ) {
    }
}
