package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单卷工艺配置草稿保存入参。
 */
@Data
public class ProcessConfigDraftSaveDTO {

    @NotNull(message = "工艺配置不能为空")
    @Valid
    private FinishConfigSaveDTO config;
}
