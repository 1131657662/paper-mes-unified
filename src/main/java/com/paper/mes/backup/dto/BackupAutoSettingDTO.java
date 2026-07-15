package com.paper.mes.backup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BackupAutoSettingDTO {

    @NotNull(message = "自动备份开关不能为空")
    private Boolean enabled;

    @NotBlank(message = "自动备份时间不能为空")
    @Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d", message = "自动备份时间格式必须为HH:mm")
    private String executionTime;
}
