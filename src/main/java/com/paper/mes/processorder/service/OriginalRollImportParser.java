package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.OriginalRollImportPreviewVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OriginalRollImportParser {

    private final DataFormatter formatter = new DataFormatter();

    public OriginalRollImportPreviewVO parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("导入文件不能为空");
        }
        if (isCsv(file)) {
            return parseCsv(file);
        }
        try (InputStream input = file.getInputStream(); Workbook workbook = new XSSFWorkbook(input)) {
            return parseWorkbook(workbook);
        } catch (IOException e) {
            throw new BusinessException("导入文件读取失败，请确认是xlsx格式");
        }
    }

    private OriginalRollImportPreviewVO parseCsv(MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "");
            List<String[]> rows = text.lines()
                    .filter(StringUtils::hasText)
                    .map(line -> line.split(",", -1))
                    .toList();
            if (rows.isEmpty()) {
                throw new BusinessException("导入模板缺少表头");
            }
            Map<String, Integer> columns = csvColumns(rows.get(0));
            OriginalRollImportPreviewVO preview = new OriginalRollImportPreviewVO();
            for (int index = 1; index < rows.size(); index++) {
                parseCsvRow(rows.get(index), index + 1, columns, preview);
            }
            return preview;
        } catch (IOException e) {
            throw new BusinessException("导入文件读取失败");
        }
    }

    private OriginalRollImportPreviewVO parseWorkbook(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new BusinessException("导入模板缺少表头");
        }
        Map<String, Integer> columns = columns(header);
        OriginalRollImportPreviewVO preview = new OriginalRollImportPreviewVO();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isBlankRow(row)) {
                continue;
            }
            parseRow(row, rowIndex + 1, columns, preview);
        }
        return preview;
    }

    private Map<String, Integer> columns(Row header) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : header) {
            String label = value(cell).replace(" ", "");
            if (StringUtils.hasText(label)) {
                columns.put(label, cell.getColumnIndex());
            }
        }
        return columns;
    }

    private void parseRow(Row row, int rowNumber, Map<String, Integer> columns, OriginalRollImportPreviewVO preview) {
        Map<String, String> raw = rawRow(row, columns);
        try {
            OriginalRollDTO dto = new OriginalRollDTO();
            dto.setPaperName(text(row, columns, "品名"));
            dto.setGramWeight(integer(row, columns, "克重"));
            dto.setOriginalWidth(integer(row, columns, "门幅"));
            dto.setRollNo(text(row, columns, "卷号"));
            dto.setRollWeight(decimal(row, columns, "单重"));
            dto.setExtraNo(text(row, columns, "编号"));
            dto.setBatchNo(text(row, columns, "批次"));
            dto.setOriginalDiameter(integer(row, columns, "直径"));
            dto.setCoreDiameter(integer(row, columns, "纸芯"));
            dto.setDamageDesc(text(row, columns, "损伤"));
            dto.setRemark(text(row, columns, "备注"));
            dto.setPieceNum(defaultInt(integer(row, columns, "件数"), 1));
            validateRow(dto, rowNumber, raw, preview);
        } catch (NumberFormatException e) {
            addError(preview, rowNumber, "数字格式", "克重、门幅、直径、纸芯、件数、单重必须是数字", raw);
        }
    }

    private void parseCsvRow(String[] row, int rowNumber, Map<String, Integer> columns, OriginalRollImportPreviewVO preview) {
        Map<String, String> raw = rawCsvRow(row, columns);
        try {
            OriginalRollDTO dto = new OriginalRollDTO();
            dto.setPaperName(csvText(row, columns, "品名"));
            dto.setGramWeight(csvInteger(row, columns, "克重"));
            dto.setOriginalWidth(csvInteger(row, columns, "门幅"));
            dto.setRollNo(csvText(row, columns, "卷号"));
            dto.setRollWeight(csvDecimal(row, columns, "单重"));
            dto.setExtraNo(csvText(row, columns, "编号"));
            dto.setBatchNo(csvText(row, columns, "批次"));
            dto.setOriginalDiameter(csvInteger(row, columns, "直径"));
            dto.setCoreDiameter(csvInteger(row, columns, "纸芯"));
            dto.setDamageDesc(csvText(row, columns, "损伤"));
            dto.setRemark(csvText(row, columns, "备注"));
            dto.setPieceNum(defaultInt(csvInteger(row, columns, "件数"), 1));
            validateRow(dto, rowNumber, raw, preview);
        } catch (NumberFormatException e) {
            addError(preview, rowNumber, "数字格式", "克重、门幅、直径、纸芯、件数、单重必须是数字", raw);
        }
    }

    private void validateRow(OriginalRollDTO dto, int rowNumber, Map<String, String> raw,
                             OriginalRollImportPreviewVO preview) {
        if (!StringUtils.hasText(dto.getPaperName())) {
            addError(preview, rowNumber, "品名", "品名不能为空", raw);
        }
        if (dto.getGramWeight() == null || dto.getGramWeight() <= 0) {
            addError(preview, rowNumber, "克重", "克重必须大于0", raw);
        }
        if (dto.getOriginalWidth() == null || dto.getOriginalWidth() <= 0) {
            addError(preview, rowNumber, "门幅", "门幅必须大于0", raw);
        }
        if (dto.getRollWeight() == null || dto.getRollWeight().signum() <= 0) {
            addError(preview, rowNumber, "单重", "单重必须大于0", raw);
        }
        if (preview.getErrors().stream().noneMatch(error -> error.getRowNumber() == rowNumber)) {
            dto.setProcessMode(1);
            dto.setMainStepType(2);
            preview.getValidRows().add(dto);
        }
    }

    private void addError(OriginalRollImportPreviewVO preview, int rowNumber, String field,
                          String message, Map<String, String> raw) {
        OriginalRollImportPreviewVO.ImportError error = new OriginalRollImportPreviewVO.ImportError();
        error.setRowNumber(rowNumber);
        error.setField(field);
        error.setMessage(message);
        error.setRaw(raw);
        preview.getErrors().add(error);
    }

    private Map<String, String> rawRow(Row row, Map<String, Integer> columns) {
        Map<String, String> raw = new LinkedHashMap<>();
        columns.forEach((label, index) -> raw.put(label, value(row.getCell(index))));
        return raw;
    }

    private Map<String, Integer> csvColumns(String[] header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < header.length; index++) {
            String label = header[index].trim().replace(" ", "");
            if (StringUtils.hasText(label)) {
                columns.put(label, index);
            }
        }
        return columns;
    }

    private Map<String, String> rawCsvRow(String[] row, Map<String, Integer> columns) {
        Map<String, String> raw = new LinkedHashMap<>();
        columns.forEach((label, index) -> raw.put(label, index < row.length ? row[index].trim() : ""));
        return raw;
    }

    private String text(Row row, Map<String, Integer> columns, String label) {
        Integer index = columns.get(label);
        return index == null ? null : value(row.getCell(index));
    }

    private Integer integer(Row row, Map<String, Integer> columns, String label) {
        String value = text(row, columns, label);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return new BigDecimal(value).intValue();
    }

    private BigDecimal decimal(Row row, Map<String, Integer> columns, String label) {
        String value = text(row, columns, label);
        return StringUtils.hasText(value) ? new BigDecimal(value) : null;
    }

    private String csvText(String[] row, Map<String, Integer> columns, String label) {
        Integer index = columns.get(label);
        return index == null || index >= row.length ? null : row[index].trim();
    }

    private Integer csvInteger(String[] row, Map<String, Integer> columns, String label) {
        String value = csvText(row, columns, label);
        return StringUtils.hasText(value) ? new BigDecimal(value).intValue() : null;
    }

    private BigDecimal csvDecimal(String[] row, Map<String, Integer> columns, String label) {
        String value = csvText(row, columns, label);
        return StringUtils.hasText(value) ? new BigDecimal(value) : null;
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (StringUtils.hasText(value(cell))) {
                return false;
            }
        }
        return true;
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null || value < 1 ? fallback : value;
    }

    private String value(Cell cell) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private boolean isCsv(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase().endsWith(".csv");
    }
}
