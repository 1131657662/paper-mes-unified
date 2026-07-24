package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.dto.ProcessStepBatchResultVO;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceStepBatchUpsertWriterTest {

    private ProcessStepMapper stepMapper;
    private ProcessCatalogStepValidator catalogValidator;
    private ServiceStepBatchUpsertWriter writer;

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProcessStep.class);
    }

    @BeforeEach
    void setUp() {
        stepMapper = mock(ProcessStepMapper.class);
        catalogValidator = mock(ProcessCatalogStepValidator.class);
        writer = new ServiceStepBatchUpsertWriter(stepMapper, mock(MachineMapper.class), catalogValidator);
        when(catalogValidator.validate(any(ProcessStep.class), any(OriginalRoll.class))).thenReturn(catalog());
        when(stepMapper.insert(any(ProcessStep.class))).thenReturn(1);
        when(stepMapper.updateById(any(ProcessStep.class))).thenReturn(1);
    }

    @Test
    void upsert_updatesExistingAndCreatesMissingStep() {
        ProcessStep existing = existingStep();
        when(stepMapper.selectList(any())).thenReturn(List.of(existing));

        ProcessStepBatchResultVO result = writer.upsert("order-1",
                List.of(standardRequest("roll-1"), standardRequest("roll-2")),
                Map.of("roll-1", roll("roll-1"), "roll-2", roll("roll-2")));

        assertThat(result.getCreatedCount()).isEqualTo(1);
        assertThat(result.getUpdatedCount()).isEqualTo(1);
        assertThat(existing.getBillingMode()).isEqualTo(1);
        assertThat(existing.getBillingAmount()).isNull();
        assertThat(existing.getUnitPrice()).isEqualByComparingTo("20");
        verify(stepMapper).updateById(existing);
        ArgumentCaptor<ProcessStep> inserted = ArgumentCaptor.forClass(ProcessStep.class);
        verify(stepMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getOriginalUuid()).isEqualTo("roll-2");
    }

    @Test
    void upsert_fixedAmount_clearsQuantityPricingFields() {
        ProcessStep existing = existingStep();
        when(stepMapper.selectList(any())).thenReturn(List.of(existing));
        ProcessStepDTO request = standardRequest("roll-1");
        request.setBillingMode(3);
        request.setBillingAmount(new BigDecimal("45.50"));

        writer.upsert("order-1", List.of(request), Map.of("roll-1", roll("roll-1")));

        assertThat(existing.getBillingAmount()).isEqualByComparingTo("45.50");
        assertThat(existing.getBillingBasis()).isNull();
        assertThat(existing.getUnitPrice()).isNull();
        assertThat(existing.getServiceQuantity()).isNull();
    }

    @Test
    void upsert_serviceStep_doesNotInheritMainProcessMachine() {
        ProcessStepDTO request = standardRequest("roll-1");
        OriginalRoll roll = roll("roll-1");
        roll.setMachineUuid("rewind-machine");

        when(stepMapper.selectList(any())).thenReturn(List.of());

        writer.upsert("order-1", List.of(request), Map.of("roll-1", roll));

        ArgumentCaptor<ProcessStep> inserted = ArgumentCaptor.forClass(ProcessStep.class);
        verify(stepMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getMachineUuid()).isNull();
    }

    @Test
    void upsert_rejectsDuplicateTargets() {
        ProcessStepDTO request = standardRequest("roll-1");

        assertThatThrownBy(() -> writer.upsert("order-1", List.of(request, request),
                Map.of("roll-1", roll("roll-1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能重复提交");
    }

    @Test
    void upsert_whenExistingStepHasConcurrentChange_rejectsBatch() {
        ProcessStep existing = existingStep();
        when(stepMapper.selectList(any())).thenReturn(List.of(existing));
        when(stepMapper.updateById(existing)).thenReturn(0);

        assertThatThrownBy(() -> writer.upsert("order-1",
                List.of(standardRequest("roll-1")), Map.of("roll-1", roll("roll-1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("刷新后重试");
    }

    private ProcessStep existingStep() {
        ProcessStep step = new ProcessStep();
        step.setUuid("step-1");
        step.setOrderUuid("order-1");
        step.setOriginalUuid("roll-1");
        step.setStepType(3);
        step.setStepSort(1);
        step.setIsMain(0);
        step.setBillingMode(3);
        step.setBillingAmount(new BigDecimal("7.50"));
        return step;
    }

    private ProcessStepDTO standardRequest(String originalUuid) {
        ProcessStepDTO dto = new ProcessStepDTO();
        dto.setOriginalUuid(originalUuid);
        dto.setStepType(3);
        dto.setBillingMode(1);
        dto.setBillingBasis("PIECE");
        dto.setUnitPrice(new BigDecimal("20"));
        return dto;
    }

    private OriginalRoll roll(String uuid) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setPieceNum(1);
        roll.setRollWeight(new BigDecimal("1000"));
        return roll;
    }

    private ProcessCatalogVO catalog() {
        return new ProcessCatalogVO("catalog-3", 3, "STRIP_SORT", "剥损整理",
                "SERVICE", "SERVICE_QUANTITY", false, true, false, List.of(), List.of(1, 3, 4));
    }
}
