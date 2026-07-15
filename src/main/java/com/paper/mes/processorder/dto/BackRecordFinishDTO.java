package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单件成品回录明细：车间实际成品重量与报废/异常信息。
 */
@Data
public class BackRecordFinishDTO {

    /** 已预生成成品传 uuid；现场定尺新增成品时为空，由后端生成卷号。 */
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
    private BigDecimal scrapWeight;
    /** 0正品 1边角余料 */
    private Integer isRemain;
    /** 是否异常次品 */
    private Integer isAbnormal;
    private String abnormalType;
    private String actualRemark;
}
