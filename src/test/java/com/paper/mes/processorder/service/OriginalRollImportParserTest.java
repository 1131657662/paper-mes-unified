package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.OriginalRollImportPreviewVO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OriginalRollImportParserTest {

    private final OriginalRollImportParser parser = new OriginalRollImportParser();

    @Test
    void parse_whenCsvRollNoIsBlank_acceptsOriginalRoll() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "original-rolls.csv",
                "text/csv",
                csvWithBlankRollNo().getBytes(StandardCharsets.UTF_8)
        );

        OriginalRollImportPreviewVO preview = parser.parse(file);

        assertTrue(preview.getErrors().isEmpty());
        assertEquals(1, preview.getValidRows().size());
        assertEquals("", preview.getValidRows().getFirst().getRollNo());
    }

    @Test
    void parse_whenCsvRequiredFieldsMissing_reportsErrors() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "original-rolls.csv",
                "text/csv",
                "品名,克重,门幅,卷号,单重,件数\n,0,,R001,0,1\n".getBytes(StandardCharsets.UTF_8)
        );

        OriginalRollImportPreviewVO preview = parser.parse(file);

        assertEquals(4, preview.getErrors().size());
    }

    @Test
    void parse_whenCsvExceedsRowLimit_rejectsImport() {
        String csv = "品名,克重,门幅,卷号,单重\n" + "纸品,80,1000,R001,10\n".repeat(501);
        MockMultipartFile file = new MockMultipartFile(
                "file", "original-rolls.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThrows(BusinessException.class, () -> parser.parse(file));
    }

    @Test
    void parse_whenCsvContainsQuotedCommaAndDoubleQuote_preservesFieldValues() {
        String csv = "品名,克重,门幅,卷号,单重,损伤,备注\n"
                + "\"牛卡,高强\",120,2500,R001,3255,\"边部\"\"轻伤\"\"\",正常\n";
        MockMultipartFile file = csvFile(csv);

        OriginalRollImportPreviewVO preview = parser.parse(file);

        assertTrue(preview.getErrors().isEmpty());
        assertEquals("牛卡,高强", preview.getValidRows().getFirst().getPaperName());
        assertEquals("边部\"轻伤\"", preview.getValidRows().getFirst().getDamageDesc());
    }

    @Test
    void parse_whenCsvContainsQuotedLineBreak_preservesMultilineRemark() {
        String csv = "品名,克重,门幅,卷号,单重,备注\n"
                + "牛卡,120,2500,R001,3255,\"第一行\n第二行\"\n";
        MockMultipartFile file = csvFile(csv);

        OriginalRollImportPreviewVO preview = parser.parse(file);

        assertTrue(preview.getErrors().isEmpty());
        assertEquals("第一行\n第二行", preview.getValidRows().getFirst().getRemark());
    }

    private MockMultipartFile csvFile(String csv) {
        return new MockMultipartFile(
                "file", "original-rolls.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
    }

    private String csvWithBlankRollNo() {
        return "品名,克重,门幅,卷号,单重,编号,批次,直径,纸芯,件数,损伤,备注\n"
                + "牛卡纸,450,2500,,3255,A001,B001,120,3,1,,\n";
    }
}
