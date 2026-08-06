package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.dto.DeliverySortSpec;
import com.paper.mes.delivery.entity.DeliveryOrder;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.paper.mes.delivery.service.DeliveryExportText.*;

@Service
public class DeliveryExportService {

    private static final int COLUMN_COUNT = 35;
    private static final int ORIGINAL_COLUMN_START = 13;
    private static final int DELIVERY_COLUMN_START = 29;

    public Workbook buildWorkbook(DeliveryDetailVO detail) {
        return buildWorkbook(detail, null);
    }

    public Workbook buildWorkbook(DeliveryDetailVO detail, DeliveryCustomerRevisionPreviewVO customerSpecs) {
        return buildWorkbook(detail, customerSpecs, List.of());
    }

    public Workbook buildWorkbook(DeliveryDetailVO detail, DeliveryCustomerRevisionPreviewVO customerSpecs,
                                  List<DeliverySortSpec> sortChain) {
        return buildWorkbook(detail, customerSpecs, sortChain, List.of(), List.of(), "physical");
    }

    public Workbook buildWorkbook(DeliveryDetailVO detail, DeliveryCustomerRevisionPreviewVO customerSpecs,
                                  List<DeliverySortSpec> physicalSortChain,
                                  List<DeliverySortSpec> customerSortChain,
                                  List<DeliverySortSpec> traceSortChain,
                                  String documentView) {
        Workbook workbook = new XSSFWorkbook();
        List<DeliveryDetailItemVO> orderedDetails = DeliveryDetailSortPolicy.sort(detail.getDetails(), physicalSortChain);
        detail.setDetails(orderedDetails);
        Sheet sheet = workbook.createSheet("出库单");
        CellStyle titleStyle = titleStyle(workbook);
        CellStyle headerStyle = headerStyle(workbook);
        CellStyle sourceStyle = workbook.createCellStyle();
        sourceStyle.setWrapText(true);
        sourceStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
        writeSummary(sheet, detail.getOrder(), titleStyle);
        writeHeader(sheet, headerStyle);
        writeItems(sheet, orderedDetails, sourceStyle);
        autosize(sheet, COLUMN_COUNT);
        if (customerSpecs != null) {
            DeliveryCustomerExportWriter.write(workbook.createSheet("客户单据"), detail, customerSpecs,
                    customerSortChain, traceSortChain, documentView);
        }
        return workbook;
    }

    private void writeSummary(Sheet sheet, DeliveryOrder order, CellStyle titleStyle) {
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("出库单明细");
        title.getCell(0).setCellStyle(titleStyle);
        row(sheet, 1, "出库单号", order.getDeliveryNo(), "货主", order.getCustomerName());
        row(sheet, 2, "客户", order.getReceiverCustomerName(), "状态", statusText(order.getDeliveryStatus()));
        row(sheet, 3, "出库日期", text(order.getDeliveryDate()), "出库仓库", order.getWarehouseName());
        row(sheet, 4, "提货人", order.getPickerName(), "车牌/柜号", join(order.getCarNo(), order.getContainerNo()));
        row(sheet, 5, "签收人", order.getSignUser(), "签收时间", text(order.getSignTime()));
        row(sheet, 6, "总件数", text(order.getTotalCount()), "实物出库总重量kg", text(order.getTotalWeight()));
        row(sheet, 7, "备注", order.getRemark(), "", "");
    }

    private void writeHeader(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(8);
        String[] labels = {
                "序号", "加工单号", "卷号", "实物品名", "实物克重", "实物规格", "规格单位",
                "实物直径", "直径单位", "实物纸芯", "纸芯单位", "实物件重kg", "实物出库重量kg",
                "母卷序号", "母卷卷号", "母卷编号", "母卷品名",
                "母卷标称克重", "母卷实际克重", "母卷标称门幅", "母卷实际门幅", "母卷标称重量kg",
                "母卷实际重量kg", "母卷加工方式", "母卷主工艺", "母卷机台", "母卷操作员", "母卷备注",
                "历史原纸信息", "加工方式", "工艺摘要", "来源", "成品状态", "备注", "回录备注"
        };
        for (int i = 0; i < labels.length; i++) {
            row.createCell(i).setCellValue(labels[i]);
            row.getCell(i).setCellStyle(style);
        }
    }

    private void writeItems(Sheet sheet, List<DeliveryDetailItemVO> details, CellStyle sourceStyle) {
        int rowIndex = 9;
        int index = 1;
        for (DeliveryDetailItemVO item : details) {
            Row row = sheet.createRow(rowIndex++);
            Object[] values = {index++, item.getOrderNo(), finishRollNoText(item), item.getPaperName(),
                    item.getGramWeight(), item.getFinishWidth(), widthUnit(item.getFinishWidth()),
                    item.getFinishDiameter(), diameterUnit(item.getFinishDiameter()),
                    item.getFinishCoreDiameter(), coreDiameterUnit(item.getFinishCoreDiameter()),
                    item.getActualWeight(), item.getOutWeight()};
            writeCells(row, 0, values);
            writeOriginalCells(row, item, sourceStyle);
            writeDeliveryCells(row, item);
        }
    }

    private void writeOriginalCells(Row row, DeliveryDetailItemVO item, CellStyle style) {
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
                originalValues(originals, source -> originalProcessModeText(source.getProcessMode())),
                originalValues(originals, source -> originalStepTypeText(source.getMainStepType())),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getMachineName),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getOperator),
                originalValues(originals, DeliveryDetailItemVO.OriginalSourceItem::getRemark),
                originals.isEmpty() ? originalSnapshotText(item) : "-"};
        for (int index = 0; index < values.length; index++) {
            row.createCell(ORIGINAL_COLUMN_START + index).setCellValue(text(values[index]));
            row.getCell(ORIGINAL_COLUMN_START + index).setCellStyle(style);
        }
        resizeForOriginals(row, originals.size());
    }

    private void writeDeliveryCells(Row row, DeliveryDetailItemVO item) {
        Object[] values = {item.getProcessModeText(), item.getProcessSummary(), sourceText(item.getSourceType()),
                finishStatusText(item.getFinishStatus()), item.getRemark(), item.getActualRemark()};
        writeCells(row, DELIVERY_COLUMN_START, values);
    }

    private void writeCells(Row row, int start, Object[] values) {
        for (int index = 0; index < values.length; index++) {
            row.createCell(start + index).setCellValue(text(values[index]));
        }
    }

    private void row(Sheet sheet, int rowIndex, String k1, String v1, String k2, String v2) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(k1);
        row.createCell(1).setCellValue(text(v1));
        row.createCell(3).setCellValue(k2);
        row.createCell(4).setCellValue(text(v2));
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private List<DeliveryDetailItemVO.OriginalSourceItem> sortedOriginals(DeliveryDetailItemVO item) {
        if (item.getOriginalItems() == null) return List.of();
        return item.getOriginalItems().stream()
                .sorted(Comparator.comparing(DeliveryDetailItemVO.OriginalSourceItem::getRowSort,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private String originalValues(List<DeliveryDetailItemVO.OriginalSourceItem> originals,
                                  Function<DeliveryDetailItemVO.OriginalSourceItem, Object> extractor) {
        if (originals.isEmpty()) return "-";
        return originals.stream().map(extractor).map(this::originalValueText).collect(Collectors.joining("\n"));
    }

    private String originalValueText(Object value) {
        if (value instanceof String string && string.isBlank()) return "-";
        return text(value);
    }

    private String originalProcessModeText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 2 -> "现场定尺"; case 3 -> "直发"; case 4 -> "仅附加工艺"; default -> "标准加工"; };
    }

    private String originalStepTypeText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 1 -> "锯纸"; case 2 -> "复卷"; default -> value.toString(); };
    }

    private void resizeForOriginals(Row row, int originalCount) {
        if (originalCount < 2) return;
        float height = row.getSheet().getDefaultRowHeightInPoints() * originalCount;
        row.setHeightInPoints(Math.min(height, 409));
    }

    private void autosize(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 12000));
        }
    }

}
