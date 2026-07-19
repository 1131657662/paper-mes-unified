package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProcessStepPricingBatchDTO {

    @NotNull(message = "加工单版本不能为空")
    @Min(value = 1, message = "加工单版本不正确")
    private Integer expectedOrderVersion;

    @NotBlank(message = "计价调整原因不能为空")
    @Size(max = 255, message = "计价调整原因不能超过255个字符")
    private String reason;

    @Size(max = 64, message = "请求标识不能超过64个字符")
    private String requestId;

    @NotEmpty(message = "至少选择一组工序")
    @Size(max = 2, message = "计价分组不能超过2组")
    @Valid
    private List<Group> groups;

    @Data
    public static class Group {
        @NotNull(message = "工序类型不能为空")
        @Min(value = 1, message = "工序类型不正确")
        @Max(value = 2, message = "工序类型不正确")
        private Integer stepType;

        @NotEmpty(message = "至少选择一道工序")
        @Size(max = 1000, message = "单次最多核定1000道工序")
        private List<@NotBlank(message = "工序ID不能为空") String> stepUuids;

        @NotNull(message = "是否恢复标准单价不能为空")
        private Boolean restoreStandard;

        @DecimalMin(value = "0.0001", message = "核定单价必须大于0")
        private BigDecimal billingUnitPrice;
    }
}
