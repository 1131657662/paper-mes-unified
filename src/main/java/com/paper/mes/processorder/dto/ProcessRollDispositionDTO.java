package com.paper.mes.processorder.dto;

import com.paper.mes.processorder.model.ProcessRollDispositionAction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Command for disposing one unrecorded mother roll after issue. */
@Data
public class ProcessRollDispositionDTO {

    @NotNull(message = "处置动作不能为空")
    private ProcessRollDispositionAction action;

    @NotBlank(message = "幂等请求号不能为空")
    @Size(max = 64, message = "幂等请求号不能超过64个字符")
    private String requestId;

    @NotBlank(message = "处置原因不能为空")
    @Size(max = 500, message = "处置原因不能超过500个字符")
    private String reason;

    @Size(max = 64, message = "入库仓库标识不能超过64个字符")
    private String warehouseUuid;

    /** Required only for DIRECT_SHIP; it is the measured total weight of this roll. */
    @DecimalMin(value = "0.001", message = "直发重量必须大于0")
    private BigDecimal actualWeight;

    @NotNull(message = "单据版本不能为空")
    private Integer expectedOrderVersion;
}
