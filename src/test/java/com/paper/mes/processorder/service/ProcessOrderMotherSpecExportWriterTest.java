package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessOrderMotherSpecExportWriterTest {

    private final ProcessOrderExportService service = new ProcessOrderExportService();

    @Test
    void buildWorkbook_withMultipleSources_usesSourceWeightAndDynamicColumns() throws IOException {
        ProcessOrderDetailVO detail = multiSourceDetail();

        try (Workbook workbook = service.buildWorkbook(detail)) {
            var sheet = workbook.getSheet("母卷加工规格");

            assertEquals("成品规格1", sheet.getRow(0).getCell(26).getStringCellValue());
            assertEquals("成品卷号2", sheet.getRow(0).getCell(31).getStringCellValue());
            assertEquals("500", sheet.getRow(1).getCell(26).getStringCellValue());
            assertEquals("300", sheet.getRow(1).getCell(27).getStringCellValue());
            assertEquals("A000187", sheet.getRow(1).getCell(28).getStringCellValue());
            assertEquals("700", sheet.getRow(1).getCell(29).getStringCellValue());
            assertEquals("A000188", sheet.getRow(1).getCell(31).getStringCellValue());
            assertEquals("200", sheet.getRow(2).getCell(27).getStringCellValue());
        }
    }

    @Test
    void buildWorkbook_withNonDeliverableFinishes_excludesThemFromDynamicColumns() throws IOException {
        ProcessOrderDetailVO.FinishProductionVO formal = finish("finish-1", "A000187", 500);
        ProcessOrderDetailVO.FinishProductionVO remain = finish("finish-2", "A000188", 100);
        remain.setIsRemain(1);
        ProcessOrderDetailVO.FinishProductionVO spare = finish("finish-3", "A000189", 300);
        spare.setIsSpare(1);
        ProcessOrderDetailVO.FinishProductionVO voided = finish("finish-4", "A000190", 400);
        voided.setRollNoStatus(3);
        ProcessOrderDetailVO detail = detail(List.of(roll("roll-1", "M001", 1000)),
                List.of(production("roll-1", List.of(formal, remain, spare, voided))));

        try (Workbook workbook = service.buildWorkbook(detail)) {
            var sheet = workbook.getSheet("母卷加工规格");

            assertEquals(29, sheet.getRow(0).getLastCellNum());
            assertEquals("A000187", sheet.getRow(1).getCell(28).getStringCellValue());
        }
    }

    private ProcessOrderDetailVO multiSourceDetail() {
        OriginalRoll first = roll("roll-1", "M001", 1000);
        OriginalRoll second = roll("roll-2", "M002", 800);
        ProcessOrderDetailVO.FinishProductionVO shared = finish("finish-1", "A000187", 500);
        shared.setActualWeight(new BigDecimal("900"));
        shared.setSources(List.of(source("roll-1", "300"), source("roll-2", "200")));
        ProcessOrderDetailVO.FinishProductionVO secondFinish = finish("finish-2", "A000188", 700);
        secondFinish.setActualWeight(new BigDecimal("400"));
        secondFinish.setSources(List.of(source("roll-1", "400"), source("roll-2", "200")));
        return detail(List.of(first, second), List.of(
                production("roll-1", List.of(shared, secondFinish)),
                production("roll-2", List.of(shared))));
    }

    private ProcessOrderDetailVO detail(List<OriginalRoll> rolls,
                                        List<ProcessOrderDetailVO.RollProductionVO> productions) {
        ProcessOrder order = new ProcessOrder();
        order.setOrderNo("JG202608170001");
        ProcessOrderDetailVO detail = new ProcessOrderDetailVO();
        detail.setOrder(order);
        detail.setOriginalRolls(rolls);
        detail.setRollProductions(productions);
        detail.setFinishRolls(List.of());
        detail.setSteps(List.of());
        return detail;
    }

    private OriginalRoll roll(String uuid, String rollNo, int width) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setRollNo(rollNo);
        roll.setOriginalWidth(width);
        roll.setGramWeight(80);
        return roll;
    }

    private ProcessOrderDetailVO.RollProductionVO production(
            String originalUuid, List<ProcessOrderDetailVO.FinishProductionVO> finishes) {
        ProcessOrderDetailVO.RollProductionVO production = new ProcessOrderDetailVO.RollProductionVO();
        production.setOriginalUuid(originalUuid);
        production.setFinishes(finishes);
        production.setRewindParams(List.of());
        return production;
    }

    private ProcessOrderDetailVO.FinishProductionVO finish(String uuid, String rollNo, int width) {
        ProcessOrderDetailVO.FinishProductionVO finish = new ProcessOrderDetailVO.FinishProductionVO();
        finish.setUuid(uuid);
        finish.setFinishRollNo(rollNo);
        finish.setFinishWidth(width);
        finish.setIsSpare(0);
        finish.setSources(List.of());
        return finish;
    }

    private ProcessOrderDetailVO.FinishSourceVO source(String originalUuid, String shareWeight) {
        ProcessOrderDetailVO.FinishSourceVO source = new ProcessOrderDetailVO.FinishSourceVO();
        source.setOriginalUuid(originalUuid);
        source.setShareWeight(new BigDecimal(shareWeight));
        return source;
    }
}
