package com.paper.mes.processorder.dto;

import java.util.List;

public record WorkshopInstructionVO(
        List<Integer> sourceRows,
        Integer sourceWidthMm,
        int sourcePieceCount,
        String instruction,
        String text) {

    public WorkshopInstructionVO {
        sourceRows = sourceRows == null ? List.of() : List.copyOf(sourceRows);
    }
}
