package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 成品-原纸关联表 biz_finish_original_rel。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_finish_original_rel")
public class FinishOriginalRel extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String finishUuid;
    private String originalUuid;
    private String orderUuid;
    private BigDecimal shareRatio;
    private BigDecimal shareWeight;
    private String remark;
}
