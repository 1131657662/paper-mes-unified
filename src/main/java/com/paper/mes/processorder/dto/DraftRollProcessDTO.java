package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 草稿加工方式步骤中的单卷工艺选择。 */
@Data
public class DraftRollProcessDTO {

    @NotBlank(message = "原纸UUID不能为空")
    private String originalUuid;

    @NotNull(message = "加工方式不能为空")
    private Integer processMode;

    private Integer mainStepType;
    private String machineUuid;
}
