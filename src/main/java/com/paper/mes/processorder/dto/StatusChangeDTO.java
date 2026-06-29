package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 状态变更入参（通用转态接口）。
 */
@Data
public class StatusChangeDTO {

    @NotNull(message = "目标状态不能为空")
    private Integer targetStatus;
}
