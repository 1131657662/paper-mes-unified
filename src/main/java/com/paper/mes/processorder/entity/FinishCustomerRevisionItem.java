package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_finish_customer_revision_item")
public class FinishCustomerRevisionItem extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String revisionUuid;
    private String finishUuid;
    private String physicalPaperName;
    private Integer physicalGramWeight;
    private Integer physicalFinishWidth;
    private BigDecimal physicalWeightSnapshot;
    private String customerPaperName;
    private Integer customerGramWeight;
    private Integer customerFinishWidth;
    private BigDecimal customerDisplayWeight;
    private String calculationMode;
    private BigDecimal weightOperand;
    private String formulaExpression;
    private String formulaInputs;
    private Integer roundingScale;
    private String roundingMode;
    private String zeroPolicy;
    private String remark;
}
