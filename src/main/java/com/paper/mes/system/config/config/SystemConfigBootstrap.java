package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemConfigBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.schema-bootstrap.enabled:true}")
    private boolean schemaBootstrapEnabled = true;

    @Override
    public void run(ApplicationArguments args) {
        if (schemaBootstrapEnabled) {
            createTablesIfMissing();
        }
        seedDictItems();
        seedConfigItems();
        migrateDeliveryCashConfigLabel();
    }

    private void createTablesIfMissing() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_dict_item` (
                  `uuid` varchar(36) NOT NULL,
                  `dict_type` varchar(50) NOT NULL,
                  `dict_name` varchar(80) NOT NULL,
                  `item_code` varchar(50) NOT NULL,
                  `item_name` varchar(80) NOT NULL,
                  `item_value` int DEFAULT NULL,
                  `sort_no` int NOT NULL DEFAULT 100,
                  `status` tinyint NOT NULL DEFAULT 1,
                  `built_in` tinyint NOT NULL DEFAULT 0,
                  `remark` varchar(255) DEFAULT NULL,
                  `is_deleted` tinyint NOT NULL DEFAULT 0,
                  `create_by` varchar(50) DEFAULT NULL,
                  `update_by` varchar(50) DEFAULT NULL,
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `version` int NOT NULL DEFAULT 1,
                  `ext_str1` varchar(255) DEFAULT NULL,
                  `ext_str2` varchar(255) DEFAULT NULL,
                  `ext_num1` decimal(12,3) DEFAULT NULL,
                  `ext_num2` decimal(12,3) DEFAULT NULL,
                  PRIMARY KEY (`uuid`),
                  UNIQUE KEY `uk_sys_dict_item_code` (`dict_type`, `item_code`, `is_deleted`),
                  KEY `idx_sys_dict_item_type` (`dict_type`),
                  KEY `idx_sys_dict_item_status` (`status`),
                  KEY `idx_is_deleted` (`is_deleted`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统数据字典项'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_config_item` (
                  `uuid` varchar(36) NOT NULL,
                  `config_group` varchar(50) NOT NULL,
                  `config_key` varchar(80) NOT NULL,
                  `config_name` varchar(80) NOT NULL,
                  `config_value` varchar(255) NOT NULL,
                  `value_type` varchar(20) NOT NULL DEFAULT 'string',
                  `unit` varchar(20) DEFAULT NULL,
                  `sort_no` int NOT NULL DEFAULT 100,
                  `status` tinyint NOT NULL DEFAULT 1,
                  `built_in` tinyint NOT NULL DEFAULT 0,
                  `remark` varchar(255) DEFAULT NULL,
                  `is_deleted` tinyint NOT NULL DEFAULT 0,
                  `create_by` varchar(50) DEFAULT NULL,
                  `update_by` varchar(50) DEFAULT NULL,
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `version` int NOT NULL DEFAULT 1,
                  `ext_str1` varchar(255) DEFAULT NULL,
                  `ext_str2` varchar(255) DEFAULT NULL,
                  `ext_num1` decimal(12,3) DEFAULT NULL,
                  `ext_num2` decimal(12,3) DEFAULT NULL,
                  PRIMARY KEY (`uuid`),
                  UNIQUE KEY `uk_sys_config_key` (`config_key`, `is_deleted`),
                  KEY `idx_sys_config_group` (`config_group`),
                  KEY `idx_sys_config_status` (`status`),
                  KEY `idx_is_deleted` (`is_deleted`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数配置'
                """);
    }

    private void seedDictItems() {
        seedDict("dict-settle-month", "settle_type", "结算方式", "monthly", "月结", 2, 10, "客户默认月结，单据可覆盖");
        seedDict("dict-settle-once", "settle_type", "结算方式", "once", "次结", 1, 20, "客户默认次结，单据可覆盖");
        seedDict("dict-invoice-yes", "invoice_type", "开票方式", "invoice", "开票", 1, 10, "单据需要开票");
        seedDict("dict-invoice-no", "invoice_type", "开票方式", "no_invoice", "不开票", 2, 20, "单据不需要开票");
        seedDict("dict-priority-normal", "priority", "优先级", "normal", "普通", 1, 10, "常规加工单");
        seedDict("dict-priority-urgent", "priority", "优先级", "urgent", "加急", 2, 20, "需要优先排产");
        seedDict("dict-fee-saw", "fee_type", "费用类型", "saw_fee", "锯纸费", null, 10, "按刀数计费");
        seedDict("dict-fee-rewind", "fee_type", "费用类型", "rewind_fee", "复卷费", null, 20, "按吨位计费");
        seedDict("dict-abnormal-damage", "abnormal_type", "异常类型", "damage", "损伤", null, 10, "来料或加工损伤");
        seedDict("dict-abnormal-weight", "abnormal_type", "异常类型", "weight_diff", "重量偏差", null, 20, "实重与理论重量偏差");
    }

    private void seedConfigItems() {
        seedConfig("cfg-auto-finish-config", "process", "process.autoFinishConfig", "成品配置允许自动生成", "false", "boolean", null, 5, "开启后可生成默认成品配置，但提交前仍须人工确认");
        seedConfig("cfg-spare-roll-count", "process", "process.spareRollNoCount", "默认备用卷号数量", "0", "number", "个", 10, "新建加工单提交时默认生成的备用卷号数量");
        seedConfig("cfg-weight-tolerance", "process", "process.weightTolerancePercent", "回录重量警告阈值", "3", "number", "%", 20, "超过该阈值进入 WARN，需要填写原因");
        seedConfig("cfg-weight-block-tolerance", "process", "process.weightBlockTolerancePercent", "回录重量拦截阈值", "5", "number", "%", 30, "超过该阈值进入 BLOCK，需要授权放行");
        seedConfig("cfg-pricing-auto-approve-limit", "process", "process.pricingAutoApproveLimit", "计价调整免审上限", "100.00", "number", "元", 40, "负向计价调整不超过该金额时可由计价权限直接核定");
        seedConfig("cfg-print-title", "print", "print.processOrderTitle", "加工单打印标题", "车间加工单", "string", null, 10, "打印模板页眉标题");
        seedConfig("cfg-page-size", "ui", "ui.defaultPageSize", "默认每页条数", "20", "number", "条", 10, "列表默认分页条数");
        seedConfig("cfg-company-name", "print", "print.companyName", "公司名称", "纸品加工 MES", "string", null, 20, "出库单、结算单和打印页展示");
        seedConfig("cfg-delivery-cash-block-mode", "delivery", "delivery.cashSettleBlockMode", "现结出库拦截模式", "1", "number", null, 10, "0关闭拦截，1警告放行，2强制拦截");
        seedConfig("cfg-settle-discount-auto-limit", "settle", "settle.discountAutoApproveLimit", "优惠免审上限", "1.00", "number", "元", 10, "不超过该金额的尾差可由有权限财务直接核销");
        seedConfig("cfg-settle-discount-max-amount", "settle", "settle.discountMaxAmount", "单次优惠金额上限", "500.00", "number", "元", 20, "超过该金额禁止通过收款核销");
        seedConfig("cfg-settle-discount-max-percent", "settle", "settle.discountMaxPercent", "单次优惠比例上限", "10.00", "number", "%", 30, "优惠金额占当前未收金额的最大比例");
        seedConfig("cfg-backup-management-enabled", "backup", "backup.managementEnabled", "管理端备份功能", "true", "boolean", null, 10, "控制管理员手动备份和恢复演练入口，默认开启");
        seedConfig("cfg-backup-retention-days", "backup", "backup.retentionDays", "本地备份保留天数", "30", "number", "天", 20, "每天自动清理超过保留期的备份，至少保留一份");
        seedConfig("cfg-backup-auto-enabled", "backup", "backup.autoEnabled", "自动备份", "true", "boolean", null, 30, "后端统一调度自动备份，默认开启");
        seedConfig("cfg-backup-auto-time", "backup", "backup.autoTime", "自动备份时间", "02:35", "string", null, 40, "每天自动备份执行时间，格式HH:mm");
    }

    private void seedDict(String uuid, String dictType, String dictName, String itemCode,
                          String itemName, Integer itemValue, int sortNo, String remark) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dict_item WHERE dict_type=? AND item_code=? AND is_deleted=0",
                Integer.class,
                dictType,
                itemCode
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO sys_dict_item
                (uuid, dict_type, dict_name, item_code, item_name, item_value, sort_no, status, built_in, remark, create_by, update_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, 1, ?, 'system', 'system')
                """, uuid, dictType, dictName, itemCode, itemName, itemValue, sortNo, remark);
    }

    private void seedConfig(String uuid, String group, String key, String name,
                            String value, String valueType, String unit, int sortNo, String remark) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_config_item WHERE config_key=? AND is_deleted=0",
                Integer.class,
                key
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO sys_config_item
                (uuid, config_group, config_key, config_name, config_value, value_type, unit, sort_no, status, built_in, remark, create_by, update_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, 'system', 'system')
                """, uuid, group, key, name, value, valueType, unit, sortNo, remark);
    }

    /**
     * Keep the persisted built-in label aligned with the current cash-settlement
     * risk semantics without changing the configured blocking mode.
     */
    void migrateDeliveryCashConfigLabel() {
        jdbcTemplate.update("UPDATE sys_config_item SET config_name=?, remark=?, update_by=?"
                        + " WHERE config_key=? AND built_in=1 AND is_deleted=0",
                "现结出库拦截模式",
                "0关闭拦截，1警告放行，2强制拦截",
                "system",
                "delivery.cashSettleBlockMode");
    }
}
