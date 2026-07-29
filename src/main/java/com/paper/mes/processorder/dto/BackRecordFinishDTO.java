package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单件成品回录明细：车间实际成品重量与报废/异常信息。
 */
@Data
public class BackRecordFinishDTO {

    /** 回录产出动作：PRODUCED（默认）、NOT_PRODUCED、ADDED。 */
    @Pattern(regexp = "PRODUCED|NOT_PRODUCED|ADDED", message = "成品产出动作无效")
    private String productionAction;

    /** 未产出或新增产出的调整原因。 */
    @Size(max = 255, message = "成品产出调整原因不能超过255个字符")
    private String productionAdjustmentReason;

    /** 已预生成成品传 uuid；回录新增成品时为空，由后端生成卷号。 */
    private String uuid;
    /** 历史未关联成品在回录时选择的来源母卷。 */
    private String originalUuid;

    /** 现场确认成品门幅 mm；现场定尺投入使用时必填。 */
    @Positive(message = "成品门幅必须大于0")
    private Integer finishWidth;
    /** 现场实测直径（英寸），按需修正。 */
    @Positive(message = "成品直径必须大于0")
    private Integer finishDiameter;
    /** 现场实测纸芯直径（英寸），按需修正。 */
    @Positive(message = "纸芯直径必须大于0")
    private Integer finishCoreDiameter;

    /** 车间实际成品重量 kg */
    @DecimalMin(value = "0.001", message = "成品实际重量必须大于0")
    private BigDecimal actualWeight;
    /** 报废重量 kg */
    @DecimalMin(value = "0.000", message = "报废重量不能为负数")
    private BigDecimal scrapWeight;
    /** 0正品 1边角余料 */
    @Min(value = 0, message = "余料标记只能为0或1")
    @Max(value = 1, message = "余料标记只能为0或1")
    private Integer isRemain;
    /** 是否异常次品 */
    @Min(value = 0, message = "异常标记只能为0或1")
    @Max(value = 1, message = "异常标记只能为0或1")
    private Integer isAbnormal;
    @Size(max = 50, message = "异常类型不能超过50个字符")
    private String abnormalType;
    @Size(max = 255, message = "实际备注不能超过255个字符")
    private String actualRemark;
}
