package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServiceOnlyFinishFactoryTest {

    @Test
    void create_preservesPhysicalSpecAndWaitsForActualWeight() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        OriginalRoll source = sourceRoll();

        FinishRoll finish = ServiceOnlyFinishFactory.create(order, source, 7);

        assertEquals("order-1", finish.getOrderUuid());
        assertEquals(3, finish.getSourceType());
        assertEquals("白卡", finish.getPaperName());
        assertEquals(300, finish.getGramWeight());
        assertEquals(2353, finish.getFinishWidth());
        assertEquals(new BigDecimal("2285"), finish.getEstimateWeight());
        assertEquals(1, finish.getFinishStatus());
        assertNull(finish.getActualWeight());
    }

    private OriginalRoll sourceRoll() {
        OriginalRoll source = new OriginalRoll();
        source.setPaperName("白卡");
        source.setGramWeight(300);
        source.setOriginalWidth(2353);
        source.setRollWeight(new BigDecimal("2285"));
        source.setRollNo("R-001");
        return source;
    }
}
