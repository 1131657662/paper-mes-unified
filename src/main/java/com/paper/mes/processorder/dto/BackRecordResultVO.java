package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 回录结果：整单状态、各原纸卷三级闭合校验结论、是否触发超差放行。
 */
@Data
public class BackRecordResultVO {

    private String orderUuid;
    private String orderNo;
    private Integer orderStatus;
    private LocalDateTime backRecordTime;
    /** 是否存在 >5% 超差并已授权放行 */
    private boolean overToleranceReleased;
    /** 直发卷自动生成的成品记录数 */
    private int directShipGenerated;
    /** 作废的未使用备用号数量 */
    private int voidedSpareCount;

    private List<RollCheck> rollChecks;

    @Data
    public static class RollCheck {
        private String originalUuid;
        private String rollNo;
        /** PASS / WARN / BLOCK */
        private String level;
        private BigDecimal actualWeight;
        private BigDecimal theoreticalWeight;
        private BigDecimal diffWeight;
        private BigDecimal diffRatioPct;
    }
}
