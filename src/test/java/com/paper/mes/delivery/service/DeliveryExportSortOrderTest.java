package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecVO;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.dto.DeliverySortSpec;
import com.paper.mes.delivery.entity.DeliveryOrder;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryExportSortOrderTest {
    @Test
    void buildWorkbook_appliesSameDetailOrderToPhysicalAndCustomerSheets() throws Exception {
        DeliveryDetailItemVO beta = item("detail-b", "B", "ROLL-B");
        DeliveryDetailItemVO alpha = item("detail-a", "A", "ROLL-A");
        DeliveryDetailVO detail = new DeliveryDetailVO();
        DeliveryOrder order = new DeliveryOrder();
        order.setDeliveryNo("CK-SORT-1");
        detail.setOrder(order);
        detail.setDetails(List.of(beta, alpha));
        DeliveryCustomerRevisionPreviewVO specs = specs(beta, alpha);

        try (Workbook workbook = new DeliveryExportService().buildWorkbook(
                detail, specs, List.of(new DeliverySortSpec("paperName", "asc")))) {
            assertThat(workbook.getSheet("出库单").getRow(9).getCell(2).toString()).isEqualTo("ROLL-A");
            assertThat(workbook.getSheet("出库单").getRow(10).getCell(2).toString()).isEqualTo("ROLL-B");
            assertThat(workbook.getSheet("客户单据").getRow(8).getCell(2).toString()).isEqualTo("ROLL-A");
            assertThat(workbook.getSheet("客户单据").getRow(9).getCell(2).toString()).isEqualTo("ROLL-B");
        }
    }

    private DeliveryDetailItemVO item(String uuid, String paperName, String rollNo) {
        DeliveryDetailItemVO item = new DeliveryDetailItemVO();
        item.setUuid(uuid);
        item.setFinishUuid(uuid + "-finish");
        item.setPaperName(paperName);
        item.setFinishRollNo(rollNo);
        item.setOutWeight(java.math.BigDecimal.ONE);
        return item;
    }

    private DeliveryCustomerRevisionPreviewVO specs(DeliveryDetailItemVO beta, DeliveryDetailItemVO alpha) {
        DeliveryCustomerSpecVO betaSpec = spec(beta);
        DeliveryCustomerSpecVO alphaSpec = spec(alpha);
        DeliveryCustomerRevisionPreviewVO result = new DeliveryCustomerRevisionPreviewVO();
        result.setItems(List.of(betaSpec, alphaSpec));
        return result;
    }

    private DeliveryCustomerSpecVO spec(DeliveryDetailItemVO item) {
        DeliveryCustomerSpecVO result = new DeliveryCustomerSpecVO();
        result.setDeliveryDetailUuid(item.getUuid());
        result.setFinishUuid(item.getFinishUuid());
        result.setFinishRollNo(item.getFinishRollNo());
        result.setPhysicalPaperName(item.getPaperName());
        return result;
    }
}
