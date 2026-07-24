package com.paper.mes.report.alert.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportAlertRuleSaveDTO {
    @NotBlank(message = "告警信号不能为空")
    @Size(max = 64, message = "告警信号过长")
    private String signalCode;
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 120, message = "规则名称不能超过120个字符")
    private String ruleName;
    @NotNull(message = "规则范围不能为空")
    @Min(1)
    @Max(4)
    private Integer scopeType;
    @Size(max = 36)
    private String customerUuid;
    @Size(max = 36)
    private String paperUuid;
    @Min(1)
    @Max(2)
    private Integer processType;
    @NotBlank(message = "比较符不能为空")
    @Pattern(regexp = "GT|GTE|LT|LTE", message = "比较符无效")
    private String comparisonOperator = "GTE";
    @NotNull(message = "阈值不能为空")
    @DecimalMin(value = "0", message = "阈值不能小于0")
    @DecimalMax(value = "100", message = "阈值不能大于100")
    private BigDecimal thresholdValue;
    @NotNull(message = "严重度不能为空")
    @Min(1)
    @Max(2)
    private Integer severity;
    @NotNull(message = "启用状态不能为空")
    @Min(0)
    @Max(1)
    private Integer isEnabled = 1;
    @Min(1)
    private Integer version;

    @AssertTrue(message = "规则范围字段不完整")
    public boolean isScopeValid() {
        if (scopeType == null) return true;
        if (scopeType == 1) return customerUuid == null && paperUuid == null && processType == null;
        if (scopeType == 2) return hasText(customerUuid) && paperUuid == null && processType == null;
        if (scopeType == 3) return customerUuid == null && hasText(paperUuid) && processType == null;
        return scopeType != 4 || customerUuid == null && paperUuid == null && processType != null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
