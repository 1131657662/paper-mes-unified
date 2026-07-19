package com.paper.mes.settle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_settle_collection_reminder")
public class SettleCollectionReminder extends BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String settleUuid;
    private String requestId;
    /** 1电话 2微信 3短信 4上门 5其他 */
    private Integer reminderChannel;
    /** 1已联系 2未接通 3承诺付款 4有异议 5其他 */
    private Integer reminderResult;
    private String contactName;
    private LocalDateTime reminderTime;
    private LocalDate nextFollowUpDate;
    private String operatorUuid;
    private String operatorName;
    private String remark;
}
