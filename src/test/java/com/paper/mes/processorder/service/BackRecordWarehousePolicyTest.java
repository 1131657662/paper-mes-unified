package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackRecordWarehousePolicyTest {

    private final WarehouseMapper mapper = mock(WarehouseMapper.class);
    private final BackRecordWarehousePolicy policy = new BackRecordWarehousePolicy(mapper);

    @Test
    void requireEnabled_whenWarehouseEnabled_returnsSnapshot() {
        Warehouse warehouse = warehouse(1);
        when(mapper.selectById("warehouse-1")).thenReturn(warehouse);

        var result = policy.requireEnabled("warehouse-1");

        assertThat(result.name()).isEqualTo("成品仓");
    }

    @Test
    void requireEnabled_whenWarehouseDisabled_rejectsSubmission() {
        when(mapper.selectById("warehouse-1")).thenReturn(warehouse(2));

        assertThatThrownBy(() -> policy.requireEnabled("warehouse-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已停用");
    }

    @Test
    void requireEnabled_whenWarehouseMissing_rejectsSubmission() {
        assertThatThrownBy(() -> policy.requireEnabled("missing"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    private Warehouse warehouse(int status) {
        Warehouse warehouse = new Warehouse();
        warehouse.setUuid("warehouse-1");
        warehouse.setWarehouseName("成品仓");
        warehouse.setStatus(status);
        return warehouse;
    }
}
