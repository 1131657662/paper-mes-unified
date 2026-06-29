package com.paper.mes.processorder.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class OriginalRollImportPreviewVO {

    private List<OriginalRollDTO> validRows = new ArrayList<>();
    private List<ImportError> errors = new ArrayList<>();

    @Data
    public static class ImportError {
        private int rowNumber;
        private String field;
        private String message;
        private Map<String, String> raw;
    }
}
