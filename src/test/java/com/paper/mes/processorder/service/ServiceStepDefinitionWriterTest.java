package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceStepDefinitionWriterTest {

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProcessStep.class);
    }

    @Test
    void update_includesNullableFieldsAndOptimisticLockConditions() {
        ProcessStepMapper mapper = mock(ProcessStepMapper.class);
        when(mapper.update(isNull(), any())).thenReturn(1);
        ProcessStep step = freeStep();

        ServiceStepDefinitionWriter.update(mapper, step);

        ArgumentCaptor<LambdaUpdateWrapper<ProcessStep>> captor = wrapperCaptor();
        verify(mapper).update(isNull(), captor.capture());
        assertNullablePricingFields(captor.getValue().getSqlSet());
        assertOptimisticConditions(captor.getValue().getSqlSegment());
    }

    private ProcessStep freeStep() {
        ProcessStep step = new ProcessStep();
        step.setUuid("step-1");
        step.setVersion(6);
        step.setIsDeleted(0);
        step.setStepType(3);
        step.setBillingMode(ProcessStepPricingPolicy.FREE);
        step.setBillingAmount(java.math.BigDecimal.ZERO);
        return step;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaUpdateWrapper<ProcessStep>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    }

    private void assertNullablePricingFields(String sqlSet) {
        assertThat(sqlSet).contains(
                "billing_basis", "service_quantity", "unit_price", "billing_unit_price",
                "billing_quantity", "billing_amount", "version = version + 1");
    }

    private void assertOptimisticConditions(String sqlSegment) {
        assertThat(sqlSegment).contains("uuid", "version", "is_deleted");
    }
}
