package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class OriginalRollCsvReader {

    List<String[]> read(MultipartFile file, int maxDataRows) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {
            return collectRows(parser, maxDataRows);
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            throw new BusinessException("CSV文件格式错误或读取失败");
        }
    }

    private List<String[]> collectRows(CSVParser parser, int maxDataRows) {
        List<String[]> rows = new ArrayList<>();
        for (CSVRecord record : parser) {
            if (isBlank(record)) {
                continue;
            }
            rows.add(record.values());
            if (rows.size() > maxDataRows + 1) {
                break;
            }
        }
        return rows;
    }

    private boolean isBlank(CSVRecord record) {
        for (String value : record) {
            if (StringUtils.hasText(value)) {
                return false;
            }
        }
        return true;
    }
}
