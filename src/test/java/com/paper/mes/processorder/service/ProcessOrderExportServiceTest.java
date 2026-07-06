package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessOrderExportServiceTest {

    private final ProcessOrderExportService service = new ProcessOrderExportService();

    @Test
    void buildWorkbook_whenFinishEstimateWeightMissing_fillsWeightByProductionWidth() throws IOException {
        try (Workbook workbook = service.buildWorkbook(detailWithMissingFinishWeight())) {
            var sheet = workbook.getSheet("成品明细");

            assertEquals("1688.973", sheet.getRow(1).getCell(9).getStringCellValue());
            assertEquals("1274.027", sheet.getRow(2).getCell(9).getStringCellValue());
        }
    }

    @Test
    void buildWorkbook_whenExplicitTrimExists_subtractsTrimBeforeFallbackWeight() throws IOException {
        try (Workbook workbook = service.buildWorkbook(detailWithExplicitTrimAndMissingFinishWeight())) {
            var sheet = workbook.getSheet("成品明细");

            assertEquals("500", sheet.getRow(1).getCell(9).getStringCellValue());
            assertEquals("400", sheet.getRow(2).getCell(9).getStringCellValue());
            assertEquals("100", sheet.getRow(3).getCell(9).getStringCellValue());
        }
    }

    private ProcessOrderDetailVO detailWithMissingFinishWeight() {
        ProcessOrderDetailVO detail = new ProcessOrderDetailVO();
        detail.setOrder(order());
        detail.setOriginalRolls(List.of());
        detail.setSteps(List.of());
        detail.setFinishRolls(List.of(finish("finish-1", "A000187", 1620), finish("finish-2", "A000188", 1222)));
        detail.setRollProductions(List.of(production()));
        return detail;
    }

    private ProcessOrderDetailVO detailWithExplicitTrimAndMissingFinishWeight() {
        ProcessOrderDetailVO detail = new ProcessOrderDetailVO();
        detail.setOrder(order());
        detail.setOriginalRolls(List.of());
        detail.setSteps(List.of());
        detail.setFinishRolls(List.of(
                finish("finish-1", "A000187", 500),
                finish("finish-2", "A000188", 400),
                trimFinish("trim-1", "A000189", 100, "100.000")));
        detail.setRollProductions(List.of(productionWithTrim()));
        return detail;
    }

    private ProcessOrder order() {
        ProcessOrder order = new ProcessOrder();
        order.setOrderNo("JG202607020001");
        order.setCustomerName("测试客户");
        return order;
    }

    private FinishRoll finish(String uuid, String rollNo, int width) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setFinishRollNo(rollNo);
        finish.setPaperName("蒙迪半化学浆");
        finish.setGramWeight(140);
        finish.setFinishWidth(width);
        finish.setIsSpare(0);
        return finish;
    }

    private FinishRoll trimFinish(String uuid, String rollNo, int width, String weight) {
        FinishRoll finish = finish(uuid, rollNo, width);
        finish.setIsRemain(1);
        finish.setEstimateWeight(new BigDecimal(weight));
        finish.setRemark("修边/余料");
        return finish;
    }

    private ProcessOrderDetailVO.RollProductionVO production() {
        ProcessOrderDetailVO.RollProductionVO production = new ProcessOrderDetailVO.RollProductionVO();
        production.setPaperName("蒙迪半化学浆");
        production.setGramWeight(140);
        production.setOriginalWidth(2842);
        production.setRollWeight(new BigDecimal("2963.000"));
        production.setPieceNum(1);
        production.setRewindParams(List.of());
        production.setFinishes(List.of(productionFinish("finish-1", "A000187", 1620), productionFinish("finish-2", "A000188", 1222)));
        return production;
    }

    private ProcessOrderDetailVO.RollProductionVO productionWithTrim() {
        ProcessOrderDetailVO.RollProductionVO production = new ProcessOrderDetailVO.RollProductionVO();
        production.setPaperName("蒙迪半化学浆");
        production.setGramWeight(140);
        production.setOriginalWidth(1000);
        production.setRollWeight(new BigDecimal("1000.000"));
        production.setPieceNum(1);
        production.setRewindParams(List.of());
        production.setFinishes(List.of(
                productionFinish("finish-1", "A000187", 500),
                productionFinish("finish-2", "A000188", 400),
                productionTrim("trim-1", "A000189", 100, "100.000")));
        return production;
    }

    private ProcessOrderDetailVO.FinishProductionVO productionFinish(String uuid, String rollNo, int width) {
        ProcessOrderDetailVO.FinishProductionVO finish = new ProcessOrderDetailVO.FinishProductionVO();
        finish.setUuid(uuid);
        finish.setFinishRollNo(rollNo);
        finish.setPaperName("蒙迪半化学浆");
        finish.setGramWeight(140);
        finish.setFinishWidth(width);
        finish.setIsSpare(0);
        finish.setSources(List.of());
        return finish;
    }

    private ProcessOrderDetailVO.FinishProductionVO productionTrim(String uuid, String rollNo, int width, String weight) {
        ProcessOrderDetailVO.FinishProductionVO finish = productionFinish(uuid, rollNo, width);
        finish.setIsRemain(1);
        finish.setEstimateWeight(new BigDecimal(weight));
        return finish;
    }
}
