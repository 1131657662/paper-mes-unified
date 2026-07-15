package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.OriginalRollDTO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class OriginalRollCsvRowMapper {

    Map<String, Integer> columns(String[] header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < header.length; index++) {
            String label = header[index].replace("\uFEFF", "").trim().replace(" ", "");
            if (StringUtils.hasText(label)) {
                columns.put(label, index);
            }
        }
        return columns;
    }

    OriginalRollDTO toDto(String[] row, Map<String, Integer> columns) {
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
        dto.setPieceNum(defaultPieces(integer(row, columns, "件数")));
        return dto;
    }

    Map<String, String> rawRow(String[] row, Map<String, Integer> columns) {
        Map<String, String> raw = new LinkedHashMap<>();
        columns.forEach((label, index) -> raw.put(label, index < row.length ? row[index].trim() : ""));
        return raw;
    }

    private String text(String[] row, Map<String, Integer> columns, String label) {
        Integer index = columns.get(label);
        return index == null || index >= row.length ? null : row[index].trim();
    }

    private Integer integer(String[] row, Map<String, Integer> columns, String label) {
        String value = text(row, columns, label);
        return StringUtils.hasText(value) ? new BigDecimal(value).intValue() : null;
    }

    private BigDecimal decimal(String[] row, Map<String, Integer> columns, String label) {
        String value = text(row, columns, label);
        return StringUtils.hasText(value) ? new BigDecimal(value) : null;
    }

    private int defaultPieces(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }
}
