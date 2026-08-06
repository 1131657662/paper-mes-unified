package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecVO;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.dto.DeliverySortSpec;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.paper.mes.delivery.service.DeliveryExportText.*;

final class DeliveryCustomerExportWriter {
    private static final int COLUMN_COUNT = 31;
    private static final int ORIGINAL_COLUMN_START = 15;
    private DeliveryCustomerExportWriter() {}

    static void write(Sheet sheet, DeliveryDetailVO detail,
                      DeliveryCustomerRevisionPreviewVO customerSpecs) {
        write(sheet, detail, customerSpecs, List.of(), List.of(), "physical");
    }

    static void write(Sheet sheet, DeliveryDetailVO detail,
                      DeliveryCustomerRevisionPreviewVO customerSpecs,
                      List<DeliverySortSpec> customerSortChain,
                      List<DeliverySortSpec> traceSortChain,
                      String documentView) {
        writeSummary(sheet, detail, customerSpecs);
        writeHeader(sheet);
        CellStyle sourceStyle = sheet.getWorkbook().createCellStyle();
        sourceStyle.setWrapText(true);
        sourceStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
        int rowIndex = 8;
        List<DeliverySortSpec> activeSortChain = "trace".equals(documentView) ? traceSortChain : customerSortChain;
        for (DeliveryCustomerSpecVO item : orderedSpecs(customerSpecs.getItems(), detail.getDetails(), activeSortChain)) {
            Row row = sheet.createRow(rowIndex);
            writeItem(row, rowIndex++ - 7, item);
            writeOriginalCells(row, findDetailItem(detail.getDetails(), item), sourceStyle);
        }
        autoSize(sheet);
    }

    private static List<DeliveryCustomerSpecVO> orderedSpecs(
            List<DeliveryCustomerSpecVO> specs, List<DeliveryDetailItemVO> details,
            List<DeliverySortSpec> sortChain) {
        if (specs == null || specs.isEmpty() || details == null || details.isEmpty()) {
            return specs == null ? List.of() : DeliveryCustomerSortPolicy.sort(specs, details, sortChain);
        }
        List<DeliveryCustomerSpecVO> ordered = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        for (DeliveryDetailItemVO detail : details) {
            for (int index = 0; index < specs.size(); index++) {
                DeliveryCustomerSpecVO item = specs.get(index);
                if (!used.contains(index) && matches(detail, item)) {
                    ordered.add(item);
                    used.add(index);
                    break;
                }
            }
        }
        for (int index = 0; index < specs.size(); index++) {
            if (!used.contains(index)) ordered.add(specs.get(index));
        }
        return DeliveryCustomerSortPolicy.sort(ordered, details, sortChain);
    }

    private static boolean matches(DeliveryDetailItemVO detail, DeliveryCustomerSpecVO item) {
        if (item.getDeliveryDetailUuid() != null && item.getDeliveryDetailUuid().equals(detail.getUuid())) return true;
        if (item.getFinishUuid() != null && item.getFinishUuid().equals(detail.getFinishUuid())) return true;
        return item.getOrderNo() != null && item.getOrderNo().equals(detail.getOrderNo())
                && item.getFinishRollNo() != null && item.getFinishRollNo().equals(detail.getFinishRollNo());
    }

    private static void writeSummary(Sheet sheet, DeliveryDetailVO detail,
                                     DeliveryCustomerRevisionPreviewVO customerSpecs) {
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("出库客户单据口径");
        title.getCell(0).setCellStyle(titleStyle(sheet));
        summaryRow(sheet, 1, new Object[]{"出库单号", detail.getOrder().getDeliveryNo(),
                "货主", detail.getOrder().getCustomerName()});
        summaryRow(sheet, 2, new Object[]{"客户版本", "V" + customerSpecs.getCurrentRevisionNo(),
                "状态", revisionStatus(customerSpecs)});
        summaryRow(sheet, 3, new Object[]{"实物出库重量kg", customerSpecs.getPhysicalTotalWeight(),
                "客户单据重量kg", customerSpecs.getCustomerTotalWeight()});
        summaryRow(sheet, 4, new Object[]{"重量差异kg", customerSpecs.getDifferenceWeight(),
                "用途", "仅用于客户单据展示，不影响库存与加工费结算"});
    }

    private static void summaryRow(Sheet sheet, int rowIndex, Object[] values) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(text(values[0]));
        row.createCell(1).setCellValue(text(values[1]));
        row.createCell(3).setCellValue(text(values[2]));
        row.createCell(4).setCellValue(text(values[3]));
    }

    private static void writeHeader(Sheet sheet) {
        String[] labels = {"序号", "加工单号", "成品卷号", "实物品名", "实物克重", "实物门幅",
                "实物出库重量kg", "客户品名", "客户克重", "客户门幅", "客户单据重量kg",
                "重量差异kg", "口径状态", "值来源", "客户备注", "母卷序号", "母卷卷号", "母卷编号",
                "母卷品名", "母卷标称克重", "母卷实际克重", "母卷标称门幅", "母卷实际门幅",
                "母卷标称重量kg", "母卷实际重量kg", "母卷加工方式", "母卷主工艺", "母卷机台",
                "母卷操作员", "母卷备注", "历史原纸信息"};
        Row row = sheet.createRow(7);
        writeCells(row, labels);
        CellStyle style = headerStyle(sheet);
        for (int index = 0; index < labels.length; index++) row.getCell(index).setCellStyle(style);
        sheet.createFreezePane(0, 8);
    }

    private static void writeItem(Row row, int rowNumber, DeliveryCustomerSpecVO item) {
        BigDecimal difference = difference(item.getCustomerDisplayWeight(), item.getPhysicalDeliveryWeight());
        String status = item.isSpecificationChanged() || item.isWeightChanged() ? "已调整" : "与实物一致";
        Object[] values = {rowNumber, item.getOrderNo(), item.getFinishRollNo(), item.getPhysicalPaperName(),
                item.getPhysicalGramWeight(), item.getPhysicalFinishWidth(), item.getPhysicalDeliveryWeight(),
                item.getCustomerPaperName(), item.getCustomerGramWeight(), item.getCustomerFinishWidth(),
                item.getCustomerDisplayWeight(), difference, status, sourceText(item.getValueSource()),
                item.getCustomerRemark()};
        writeCells(row, values);
    }
    private static void writeOriginalCells(Row row, DeliveryDetailItemVO item, CellStyle style) {
        List<DeliveryDetailItemVO.OriginalSourceItem> originals = sortedOriginals(item);
        Object[] values = {originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getRowSort),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getRollNo),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getExtraNo),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getPaperName),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getGramWeight),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getActualGramWeight),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getOriginalWidth),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getActualWidth),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getTotalWeight),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getActualWeight),
                originalValues(originals, source -> processModeText(source.getProcessMode())),
                originalValues(originals, source -> stepTypeText(source.getMainStepType())),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getMachineName),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getOperator),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getRemark),
                originals.isEmpty() && item != null ? originalSnapshotText(item) : "-"};
        for (int index = 0; index < values.length; index++) {
            row.createCell(ORIGINAL_COLUMN_START + index).setCellValue(text(values[index]));
            row.getCell(ORIGINAL_COLUMN_START + index).setCellStyle(style);
        }
        resizeForOriginals(row, originals.size());
    }
    private static DeliveryDetailItemVO findDetailItem(List<DeliveryDetailItemVO> details,
                                                        DeliveryCustomerSpecVO item) {
        if (details == null) return null;
        if (hasText(item.getDeliveryDetailUuid())) {
            DeliveryDetailItemVO match = details.stream().filter(detail -> item.getDeliveryDetailUuid().equals(detail.getUuid()))
                    .findFirst().orElse(null);
            if (match != null) return match;
        }
        if (hasText(item.getFinishUuid())) {
            DeliveryDetailItemVO match = details.stream().filter(detail -> item.getFinishUuid().equals(detail.getFinishUuid()))
                    .findFirst().orElse(null);
            if (match != null) return match;
        }
        if (!hasText(item.getOrderNo()) || !hasText(item.getFinishRollNo())) return null;
        return details.stream().filter(detail -> item.getOrderNo().equals(detail.getOrderNo())
                && item.getFinishRollNo().equals(detail.getFinishRollNo())).findFirst().orElse(null);
    }
    private static List<DeliveryDetailItemVO.OriginalSourceItem> sortedOriginals(DeliveryDetailItemVO item) {
        if (item == null || item.getOriginalItems() == null) return List.of();
        return item.getOriginalItems().stream().sorted(Comparator.comparing(
                DeliveryDetailItemVO.OriginalSourceItem::getRowSort, Comparator.nullsLast(Integer::compareTo))).toList();
    }
    private static String originalValues(List<DeliveryDetailItemVO.OriginalSourceItem> originals,
                                         Function<DeliveryDetailItemVO.OriginalSourceItem, Object> extractor) {
        if (originals.isEmpty()) return "-";
        return originals.stream().map(extractor).map(DeliveryCustomerExportWriter::originalValueText)
                .collect(Collectors.joining("\n"));
    }
    private static String originalValueText(Object value) {
        return value instanceof String string && string.isBlank() ? "-" : text(value);
    }
    private static String processModeText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 2 -> "现场定尺"; case 3 -> "直发"; case 4 -> "仅附加工艺"; default -> "标准加工"; };
    }
    private static String stepTypeText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 1 -> "锯纸"; case 2 -> "复卷"; default -> value.toString(); };
    }
    private static void resizeForOriginals(Row row, int originalCount) {
        if (originalCount < 2) return;
        row.setHeightInPoints(Math.min(row.getSheet().getDefaultRowHeightInPoints() * originalCount, 409));
    }
    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private static BigDecimal difference(BigDecimal customer, BigDecimal physical) {
        if (customer == null || physical == null) return null;
        return customer.subtract(physical).stripTrailingZeros();
    }
    private static String sourceText(String value) {
        if ("DELIVERY_REVISION".equals(value)) return "出库客户更正版";
        if ("HISTORICAL_BASELINE".equals(value)) return "历史出库实物基线";
        if ("FINISH_DEFAULT".equals(value)) return "加工成品客户规格";
        if ("PHYSICAL".equals(value)) return "实物口径";
        return value;
    }
    private static String revisionStatus(DeliveryCustomerRevisionPreviewVO specs) {
        String kind = specs.getCurrentRevisionKind();
        if (DeliveryCustomerRevisionPreviewService.REVISION_KIND_SYSTEM.equals(kind)) return "出库确认冻结基线";
        if (DeliveryCustomerRevisionPreviewService.REVISION_KIND_USER.equals(kind)) return "已发布客户更正版";
        if (DeliveryCustomerRevisionPreviewService.REVISION_KIND_HISTORICAL.equals(kind)) return "历史出库实物基线";
        return "继承加工成品口径";
    }
    private static void writeCells(Row row, Object[] values) {
        for (int index = 0; index < values.length; index++) {
            row.createCell(index).setCellValue(text(values[index]));
        }
    }
    private static CellStyle titleStyle(Sheet sheet) {
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }
    private static CellStyle headerStyle(Sheet sheet) {
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
    private static void autoSize(Sheet sheet) {
        for (int index = 0; index < COLUMN_COUNT; index++) {
            sheet.autoSizeColumn(index);
            sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 512, 12000));
        }
    }
}
