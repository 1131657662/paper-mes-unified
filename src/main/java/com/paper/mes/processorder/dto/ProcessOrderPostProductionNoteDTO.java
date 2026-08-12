package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** A non-production note recorded after the issued production version is frozen. */
@Data
public class ProcessOrderPostProductionNoteDTO {

    @NotNull(message = "加工单版本不能为空")
    private Integer expectedVersion;

    @Size(max = 2000, message = "后生产备注不能超过2000个字符")
    private String postProductionNote;
}
