package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackRecordVoidedFinishWriterTest {

    @Test
    void write_explicitlyClearsInventoryValues() {
        FinishRollMapper mapper = mock(FinishRollMapper.class);
        when(mapper.update(isNull(), any())).thenReturn(1);
        FinishRoll finish = voidedFinish();

        BackRecordVoidedFinishWriter.write(mapper, finish);

        ArgumentCaptor<UpdateWrapper<FinishRoll>> captor = wrapperCaptor();
        verify(mapper).update(isNull(), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains(
                "actual_weight", "remaining_weight", "scrap_weight", "stock_in_time");
        assertThat(captor.getValue().getParamNameValuePairs()).containsValue(null);
    }

    private FinishRoll voidedFinish() {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setVersion(2);
        finish.setRollNoStatus(3);
        finish.setFinishStatus(4);
        finish.setProductionResult(3);
        finish.setActualWeight(new BigDecimal("50.000"));
        return finish;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<UpdateWrapper<FinishRoll>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(UpdateWrapper.class);
    }
}
