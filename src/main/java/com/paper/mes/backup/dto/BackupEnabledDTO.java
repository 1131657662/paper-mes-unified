package com.paper.mes.backup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BackupEnabledDTO {

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
