package com.paper.mes.delivery.service;

import com.paper.mes.delivery.entity.DeliveryCustomerRevisionItem;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryCustomerRevisionItemMapper;
import com.paper.mes.delivery.mapper.DeliveryCustomerRevisionMapper;
import com.paper.mes.processorder.entity.FinishRoll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryCustomerRevisionSnapshotWriterTest {

    @Mock private DeliveryCustomerRevisionMapper revisionMapper;
    @Mock private DeliveryCustomerRevisionItemMapper itemMapper;
    @Mock private DeliveryCustomerRevisionReader revisionReader;

    @Test
    void freezeOnConfirmScalesCustomerWeightForPartialOutbound() {
        when(revisionReader.nextRevisionNo("delivery-1")).thenReturn(1);
        when(revisionMapper.insert(any(com.paper.mes.delivery.entity.DeliveryCustomerRevision.class))).thenAnswer(invocation -> {
            var revision = invocation.getArgument(0, com.paper.mes.delivery.entity.DeliveryCustomerRevision.class);
            revision.setUuid("revision-1");
            return 1;
        });

        DeliveryDetail detail = new DeliveryDetail();
        detail.setUuid("detail-1");
        detail.setDeliveryUuid("delivery-1");
        detail.setFinishUuid("finish-1");
        detail.setFinishRollNo("P000001");
        detail.setPaperName("白卡");
        detail.setOutWeight(new BigDecimal("500.000"));

        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setPaperName("白卡");
        finish.setGramWeight(265);
        finish.setFinishWidth(1000);
        finish.setCustomerPaperName("食品卡");
        finish.setCustomerGramWeight(275);
        finish.setCustomerFinishWidth(1000);
        finish.setCustomerDisplayWeight(new BigDecimal("1100.000"));
        finish.setActualWeight(new BigDecimal("1000.000"));

        DeliveryOrder order = new DeliveryOrder();
        order.setUuid("delivery-1");

        new DeliveryCustomerRevisionSnapshotWriter(revisionMapper, itemMapper, revisionReader)
                .freezeOnConfirm(order, List.of(detail), Map.of("finish-1", finish));

        ArgumentCaptor<DeliveryCustomerRevisionItem> captor = ArgumentCaptor.forClass(DeliveryCustomerRevisionItem.class);
        verify(itemMapper).insert(captor.capture());
        assertEquals("食品卡", captor.getValue().getCustomerPaperName());
        assertEquals(new BigDecimal("550.000"), captor.getValue().getCustomerDisplayWeight());
        assertEquals("KEEP", captor.getValue().getCalculationMode());
    }
}
