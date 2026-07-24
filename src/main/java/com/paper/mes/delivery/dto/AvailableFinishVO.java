package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 可出库成品视图（该客户已入库、可勾选合并提货的成品，含 source_type=2 直发原纸）。
 */
@Data
public class AvailableFinishVO {

    private String finishUuid;
    private String finishRollNo;
    private String orderUuid;
    private String orderNo;
    private LocalDate orderDate;
    private String warehouseUuid;
    private String warehouseName;
    private String warehouseLocation;
    private LocalDateTime stockInTime;
    private String paperName;
    private Integer gramWeight;
    private Integer finishWidth;
    private Integer finishDiameter;
    private Integer finishCoreDiameter;
    private BigDecimal actualWeight;
    /** 当前剩余可出库重量 kg；为空时旧数据按 actualWeight 兜底。 */
    private BigDecimal remainingWeight;
    /** 1 表示切边/余料，其他值表示正常成品。 */
    private Integer isRemain;
    /** 1加工产出 2原纸直发 3仅附加工艺产出 */
    private Integer sourceType;
    /** 1待入库 2已入库 3已出库 4报废 */
    private Integer finishStatus;
    /** 成品记录上的来源卷号快照，用于直发或旧数据兜底展示。 */
    private String originalRollNos;
    /** 成品关联的来源母卷；一个成品可能由多卷母卷合并加工而来。 */
    private List<SourceMotherRollVO> sourceMotherRolls = List.of();
    /** 1次结 2月结，来自加工单快照。 */
    private Integer settleType;
    /** 月结对账日，来自加工单快照。 */
    private Integer settleDay;
    private Integer isInvoice;
    /** 该客户是否存在未结清结算单，用于出库风险提示。 */
    private Boolean settlementRisk;

    @Data
    public static class SourceMotherRollVO {
        private String originalUuid;
        private Integer rowSort;
        private String rollNo;
        private String extraNo;
        private String paperName;
        /** 实际克重优先，缺失时回退到标称克重 g。 */
        private Integer gramWeight;
        private Integer originalWidth;
        private BigDecimal actualWeight;
        /** 该母卷分配到当前成品的重量 kg。 */
        private BigDecimal allocationWeight;
    }
}
