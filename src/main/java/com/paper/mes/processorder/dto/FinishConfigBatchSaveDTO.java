package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Batch finish configuration payload. The whole request is persisted atomically.
 */
@Data
public class FinishConfigBatchSaveDTO {

    @NotEmpty(message = "至少需要提交一条成品配置")
    @Size(max = 100, message = "单次最多提交100条成品配置")
    @Valid
    private List<FinishConfigBatchItemDTO> items;

    @Data
    public static class FinishConfigBatchItemDTO {

        @NotBlank(message = "母卷标识不能为空")
        private String rollUuid;

        @NotNull(message = "成品配置不能为空")
        @Valid
        private FinishConfigSaveDTO config;
    }
}
