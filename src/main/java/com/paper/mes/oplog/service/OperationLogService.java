package com.paper.mes.oplog.service;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.oplog.entity.OperationLog;
import com.paper.mes.oplog.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 操作日志写入服务。登录请求始终以当前登录用户作为审计主体。 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    /** 动作类型常量，对齐 DDL 注释枚举。 */
    public static final String ACTION_REPRINT = "补打";
    public static final String ACTION_BACK_RECORD = "回录";
    public static final String ACTION_VOID_ROLL_NO = "作废卷号";
    public static final String ACTION_VOID_ORDER = "作废加工单";
    public static final String ACTION_OVER_TOLERANCE_RELEASE = "超差放行";
    public static final String ACTION_WEIGHT_VARIANCE_CONFIRM = "重量偏差确认";
    public static final String ACTION_DELIVERY_CONFIRM = "出库确认";
    public static final String ACTION_DELIVERY_RELEASE = "出库放行";
    public static final String ACTION_DELIVERY_CANCEL = "取消出库";
    public static final String ACTION_SETTLE = "结算";
    public static final String ACTION_SETTLE_VOID = "作废结算";
    public static final String ACTION_RECEIVE = "收款";
    public static final String ACTION_RECEIVE_CANCEL = "取消收款";
    public static final String ACTION_COLLECTION_REMINDER = "催收提醒";
    public static final String ACTION_ROLLBACK = "回退";
    public static final String ACTION_FIELD_MODIFY = "字段修改";
    public static final String ACTION_PRICING_ADJUST = "计价调整";
    public static final String ACTION_USER_CREATE = "新增用户";
    public static final String ACTION_USER_UPDATE = "编辑用户";
    public static final String ACTION_USER_STATUS = "账号启停";
    public static final String ACTION_PASSWORD_RESET = "重置密码";
    public static final String ACTION_PASSWORD_CHANGE = "修改密码";
    public static final String ACTION_BACKUP = "数据备份";
    public static final String ACTION_BACKUP_VERIFY = "恢复演练";
    public static final String ACTION_BACKUP_DELETE = "删除备份";
    public static final String ACTION_BACKUP_CLEANUP = "清理备份";
    public static final String ACTION_DATA_REPAIR = "数据修复";

    /** 业务类型常量。 */
    public static final String BIZ_TYPE_ORDER = "加工单";
    public static final String BIZ_TYPE_DELIVERY = "出库单";
    public static final String BIZ_TYPE_SETTLE = "结算单";
    public static final String BIZ_TYPE_USER = "系统用户";
    public static final String BIZ_TYPE_SYSTEM_CONFIG = "系统配置";
    public static final String BIZ_TYPE_BACKUP = "数据安全";

    private final OperationLogMapper operationLogMapper;

    /**
     * 写入一条操作日志。
     *
     * @param bizType    业务类型，如 "加工单"
     * @param bizUuid    业务主键
     * @param bizNo      冗余业务单号，可空
     * @param actionType 动作类型，用本类 ACTION_* 常量
     * @param operator   操作人，空则落当前登录用户
     * @param remark     备注（放行原因、补打原因等）
     */
    public void record(String bizType, String bizUuid, String bizNo,
                       String actionType, String operator, String remark) {
        insert(bizType, bizUuid, bizNo, actionType, resolveOperator(operator), remark);
    }

    public void recordVerifiedActor(String bizType, String bizUuid, String bizNo,
                                    String actionType, String verifiedActor, String remark) {
        if (verifiedActor == null || verifiedActor.isBlank()) {
            throw new IllegalArgumentException("verified actor is required");
        }
        insert(bizType, bizUuid, bizNo, actionType, verifiedActor, remark);
    }

    private void insert(String bizType, String bizUuid, String bizNo,
                        String actionType, String operator, String remark) {
        OperationLog log = new OperationLog();
        log.setBizType(bizType);
        log.setBizUuid(bizUuid);
        log.setBizNo(bizNo);
        log.setActionType(actionType);
        log.setOperator(operator);
        log.setOperateTime(LocalDateTime.now());
        log.setRemark(remark);
        operationLogMapper.insert(log);
    }

    /**
     * 写入一条字段级变更日志（action_type=字段修改），由 AOP 字段审计切面调用。
     *
     * @param bizType   业务类型
     * @param bizUuid   业务主键
     * @param bizNo     冗余业务单号，可空
     * @param fieldName 变更字段名
     * @param oldValue  修改前值，可空
     * @param newValue  修改后值，可空
     * @param operator  操作人，空则落当前登录用户
     */
    public void recordField(String bizType, String bizUuid, String bizNo,
                            String fieldName, String oldValue, String newValue, String operator) {
        OperationLog log = new OperationLog();
        log.setBizType(bizType);
        log.setBizUuid(bizUuid);
        log.setBizNo(bizNo);
        log.setActionType(ACTION_FIELD_MODIFY);
        log.setFieldName(fieldName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setOperator(resolveOperator(operator));
        log.setOperateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private String resolveOperator(String operator) {
        if (AuthContextHolder.getCurrentUser() != null) {
            String current = AuthContextHolder.currentDisplayName();
            if (current != null && !current.isBlank()) {
                return current;
            }
        }
        if (operator != null && !operator.isBlank()) {
            return operator;
        }
        return "system";
    }
}
