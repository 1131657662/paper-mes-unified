package com.paper.mes.backup.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BackupRetentionDTO {

    @NotNull(message = "保留天数不能为空")
    @Min(value = 7, message = "保留天数不能少于7天")
    @Max(value = 3650, message = "保留天数不能超过3650天")
    private Integer retentionDays;
}
