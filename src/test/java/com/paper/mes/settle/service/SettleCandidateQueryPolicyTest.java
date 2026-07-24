package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.settle.dto.SettleCandidateQuery;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettleCandidateQueryPolicyTest {

    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ProcessOrder.class);
    }

    @Test
    void create_whenOrderUuidsProvided_limitsCandidatesToRequestedOrders() {
        SettleCandidateQuery query = new SettleCandidateQuery();
        query.setOrderUuids(List.of("order-101", "order-205"));

        LambdaQueryWrapper<ProcessOrder> wrapper = SettleCandidateQueryPolicy.create(query);

        assertThat(wrapper.getSqlSegment()).contains("uuid IN");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains("order-101", "order-205");
    }
}
