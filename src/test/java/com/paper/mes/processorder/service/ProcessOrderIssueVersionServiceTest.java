package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.ProcessOrderIssueVersion;
import com.paper.mes.processorder.mapper.ProcessOrderIssueVersionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessOrderIssueVersionServiceTest {

    private ProcessOrderIssueVersionMapper mapper;
    private ProcessOrderIssueVersionService service;

    @BeforeAll
    static void initializeMapperMetadata() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProcessOrderIssueVersion.class);
    }

    @BeforeEach
    void setUp() {
        mapper = mock(ProcessOrderIssueVersionMapper.class);
        service = new ProcessOrderIssueVersionService(mapper);
    }

    @Test
    void prepareWithoutExistingHistory_createsPendingVersionOneWithBeforeSnapshot() {
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(mapper.insert(any(ProcessOrderIssueVersion.class))).thenReturn(1);

        ProcessOrderIssueVersion row = service.prepare(
                "order-1", "before-json", "客户改规格", "operator", LocalDateTime.now(),
                "request-1", "payload-hash-1");

        assertThat(row.getVersionNo()).isEqualTo(1);
        assertThat(row.getStatus()).isEqualTo(ProcessOrderIssueVersion.STATUS_PENDING);
        assertThat(row.getSnapshotBefore()).isEqualTo("before-json");
        assertThat(row.getSnapshotAfter()).isNull();
        assertThat(row.getRequestId()).isEqualTo("request-1");
        assertThat(row.getPayloadHash()).isEqualTo("payload-hash-1");
        verify(mapper).insert(argThat((ProcessOrderIssueVersion value) -> "客户改规格".equals(value.getChangeReason())));
    }

    @Test
    void prepareWhenPendingAlreadyExists_rejectsSecondChange() {
        ProcessOrderIssueVersion pending = new ProcessOrderIssueVersion();
        pending.setStatus(ProcessOrderIssueVersion.STATUS_PENDING);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pending);

        assertThatThrownBy(() -> service.prepare(
                "order-1", "before-json", "再次修改", "operator", LocalDateTime.now(),
                "request-2", "payload-hash-2"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有待应用");
    }

    @Test
    void applyPendingVersion_recordsAfterSnapshotAndMarksApplied() {
        ProcessOrderIssueVersion row = new ProcessOrderIssueVersion();
        row.setStatus(ProcessOrderIssueVersion.STATUS_PENDING);
        row.setVersionNo(2);
        when(mapper.updateById(any(ProcessOrderIssueVersion.class))).thenReturn(1);

        service.apply(row, "after-json", "operator", LocalDateTime.now());

        assertThat(row.getSnapshotAfter()).isEqualTo("after-json");
        assertThat(row.getStatus()).isEqualTo(ProcessOrderIssueVersion.STATUS_APPLIED);
        verify(mapper).updateById(row);
    }
}
