package com.paper.mes.delivery.service;

import com.paper.mes.customerdisplay.formula.CustomerWeightFormulaEngine;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.entity.DeliveryCustomerRevisionItem;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.processorder.entity.FinishRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryCustomerSpecPlannerTest {

    private final DeliveryCustomerSpecPlanner planner =
            new DeliveryCustomerSpecPlanner(new CustomerWeightFormulaEngine());

    @Test
    void current_scalesFinishCustomerWeightForPartialDelivery() {
        FinishRoll finish = finish();
        finish.setActualWeight(new BigDecimal("1000"));
        finish.setCustomerDisplayWeight(new BigDecimal("1100"));
        DeliveryDetailItemVO physical = physical();
        physical.setOutWeight(new BigDecimal("500"));

        var result = planner.current(context(physical, finish, null, false));

        assertEquals(new BigDecimal("550.000"), result.getCustomerDisplayWeight());
        assertEquals("FINISH_DEFAULT", result.getValueSource());
    }

    @Test
    void current_deliveryRevisionOverridesLaterFinishDefaults() {
        DeliveryCustomerRevisionItem revision = new DeliveryCustomerRevisionItem();
        revision.setCustomerPaperName("食品卡");
        revision.setCustomerGramWeight(75);
        revision.setCustomerFinishWidth(900);
        revision.setCustomerDisplayWeight(new BigDecimal("1188"));

        var result = planner.current(context(physical(), finish(), revision, false));

        assertEquals("食品卡", result.getCustomerPaperName());
        assertEquals(new BigDecimal("1188"), result.getCustomerDisplayWeight());
        assertEquals("DELIVERY_REVISION", result.getValueSource());
    }

    @Test
    void current_completedHistoricalDelivery_usesImmutablePhysicalBaseline() {
        var result = planner.current(context(physical(), finish(), null, true));

        assertEquals("白卡", result.getCustomerPaperName());
        assertEquals(70, result.getCustomerGramWeight());
        assertEquals(new BigDecimal("1000"), result.getCustomerDisplayWeight());
        assertEquals("HISTORICAL_BASELINE", result.getValueSource());
    }

    private DeliveryCustomerSpecContext context(
            DeliveryDetailItemVO physical, FinishRoll finish,
            DeliveryCustomerRevisionItem revision, boolean usePhysicalBaseline) {
        DeliveryDetail detail = new DeliveryDetail();
        detail.setUuid("detail-1");
        detail.setVersion(1);
        return new DeliveryCustomerSpecContext(physical, detail, finish, revision, usePhysicalBaseline);
    }

    private DeliveryDetailItemVO physical() {
        DeliveryDetailItemVO item = new DeliveryDetailItemVO();
        item.setUuid("detail-1");
        item.setFinishUuid("finish-1");
        item.setFinishRollNo("A000001");
        item.setPaperName("白卡");
        item.setGramWeight(70);
        item.setFinishWidth(1000);
        item.setOutWeight(new BigDecimal("1000"));
        return item;
    }

    private FinishRoll finish() {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setPaperName("白卡");
        finish.setGramWeight(70);
        finish.setFinishWidth(1000);
        finish.setActualWeight(new BigDecimal("1000"));
        finish.setCustomerPaperName("食品卡");
        finish.setCustomerGramWeight(75);
        finish.setCustomerFinishWidth(900);
        finish.setCustomerDisplayWeight(new BigDecimal("1100"));
        return finish;
    }
}
