package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 草稿提交入参。 */
@Data
public class DraftSubmitDTO {

    @NotNull(message = "草稿版本不能为空")
    @Min(value = 0, message = "草稿版本不能小于0")
    private Integer expectedVersion;
}
