-- =============================================================================
-- 卷筒纸加工管理系统 V4.1  数据库建表脚本
-- Phase 1 / P0-1  数据库建表
-- 引擎: InnoDB   字符集: utf8mb4   排序规则: utf8mb4_general_ci
-- 规范依据: 开发文档 第三章 + 3.4 节 DDL 统一规范
--   金额 decimal(12,2)/decimal(10,2)  重量 decimal(10,3)  主键 varchar(36) UUID
--   软删除 is_deleted tinyint NOT NULL DEFAULT 0   乐观锁 version int NOT NULL DEFAULT 1
--   通用字段: create_by/update_by/create_time/update_time/version/ext_str1/ext_str2/ext_num1/ext_num2
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 一、基础档案
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 3.3.1 sys_customer 客户档案表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_customer`;
CREATE TABLE `sys_customer` (
  `uuid`              VARCHAR(36)   NOT NULL                COMMENT '客户唯一ID',
  `customer_code`     VARCHAR(50)   NOT NULL                COMMENT '客户编码',
  `customer_name`     VARCHAR(100)  NOT NULL                COMMENT '客户名称',
  `contact`           VARCHAR(50)   DEFAULT NULL            COMMENT '对接人',
  `phone`             VARCHAR(20)   DEFAULT NULL            COMMENT '联系电话',
  `settle_type`       TINYINT       NOT NULL DEFAULT 2      COMMENT '1次结 2月结',
  `settle_day`        TINYINT       DEFAULT NULL            COMMENT '月结对账日',
  `saw_price`         DECIMAL(10,2) DEFAULT NULL            COMMENT '锯纸单价 元/刀',
  `rewind_price`      DECIMAL(10,2) DEFAULT NULL            COMMENT '复卷单价 元/吨',
  `default_invoice`   TINYINT       NOT NULL DEFAULT 2      COMMENT '1开票 2不开票',
  `price_include_tax` TINYINT       NOT NULL DEFAULT 2      COMMENT '1含税 2不含税',
  `tax_rate`          DECIMAL(5,2)  DEFAULT 13.00           COMMENT '税率%',
  `tax_no`            VARCHAR(50)   DEFAULT NULL            COMMENT '税号',
  `invoice_address`   VARCHAR(255)  DEFAULT NULL            COMMENT '开票地址电话',
  `bank_account`      VARCHAR(100)  DEFAULT NULL            COMMENT '开户行账号',
  `delivery_address`  VARCHAR(255)  DEFAULT NULL            COMMENT '送货地址',
  `customer_level`    TINYINT       DEFAULT NULL            COMMENT '客户等级',
  `export_template`   VARCHAR(50)   DEFAULT NULL            COMMENT '客户Excel导入导出模板标识',
  `remark`            VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `is_deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `customer_code_active` VARCHAR(50)
    GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`customer_code`), '') ELSE NULL END)
    STORED COMMENT '启用客户唯一编码',
  `create_by`         VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`         VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`           INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`          VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`          VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`          DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`          DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  KEY `idx_customer_code` (`customer_code`),
  KEY `idx_customer_name` (`customer_name`),
  UNIQUE KEY `uk_sys_customer_active_code` (`customer_code_active`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户档案表';

-- -----------------------------------------------------------------------------
-- 3.3.x sys_paper 纸张档案表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_paper`;
CREATE TABLE `sys_paper` (
  `uuid`         VARCHAR(36)  NOT NULL                COMMENT '纸张唯一ID',
  `paper_code`   VARCHAR(50)  DEFAULT NULL            COMMENT '纸张编码',
  `paper_name`   VARCHAR(100) NOT NULL                COMMENT '纸张品名',
  `gram_weight`  INT          DEFAULT NULL            COMMENT '常用克重 g/㎡',
  `paper_type`   VARCHAR(50)  DEFAULT NULL            COMMENT '纸张类型',
  `remark`       VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `is_deleted`   TINYINT      NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `paper_code_active` VARCHAR(50)
    GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`paper_code`), '') ELSE NULL END)
    STORED COMMENT '启用纸张唯一编码',
  `create_by`    VARCHAR(50)  DEFAULT NULL            COMMENT '创建人',
  `update_by`    VARCHAR(50)  DEFAULT NULL            COMMENT '更新人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`      INT          NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`     VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`     VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`     DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值1',
  `ext_num2`     DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_sys_paper_active_code` (`paper_code_active`),
  KEY `idx_paper_name` (`paper_name`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='纸张档案表';

-- -----------------------------------------------------------------------------
-- 3.3.x sys_machine 机台档案表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_machine`;
CREATE TABLE `sys_machine` (
  `uuid`          VARCHAR(36)  NOT NULL                COMMENT '机台唯一ID',
  `machine_code`  VARCHAR(50)  DEFAULT NULL            COMMENT '机台编码',
  `machine_name`  VARCHAR(100) NOT NULL                COMMENT '机台名称',
  `machine_type`  TINYINT      DEFAULT NULL            COMMENT '机台类型 1锯纸 2复卷 3通用',
  `resource_kind` VARCHAR(20)  NOT NULL DEFAULT 'MACHINE' COMMENT 'MACHINE设备 WORKSTATION工位',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '1启用 2停用',
  `remark`        VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `machine_code_active` VARCHAR(50)
    GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`machine_code`), '') ELSE NULL END)
    STORED COMMENT '启用机台唯一编码',
  `create_by`     VARCHAR(50)  DEFAULT NULL            COMMENT '创建人',
  `update_by`     VARCHAR(50)  DEFAULT NULL            COMMENT '更新人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`       INT          NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`      VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`      VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`      DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值1',
  `ext_num2`      DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_sys_machine_active_code` (`machine_code_active`),
  KEY `idx_machine_name` (`machine_name`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `chk_machine_resource_kind` CHECK (`resource_kind` IN ('MACHINE','WORKSTATION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机台档案表';

-- -----------------------------------------------------------------------------
-- 3.3.x sys_warehouse 仓库档案表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_warehouse`;
CREATE TABLE `sys_warehouse` (
  `uuid`            VARCHAR(36)  NOT NULL                COMMENT '仓库唯一ID',
  `warehouse_code`  VARCHAR(50)  DEFAULT NULL            COMMENT '仓库编码',
  `warehouse_name`  VARCHAR(100) NOT NULL                COMMENT '仓库名称',
  `location`        VARCHAR(255) DEFAULT NULL            COMMENT '库位/地址',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '1启用 2停用',
  `is_default`      TINYINT      NOT NULL DEFAULT 0      COMMENT '默认仓库',
  `remark`          VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `warehouse_code_active` VARCHAR(50)
    GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`warehouse_code`), '') ELSE NULL END)
    STORED COMMENT '启用仓库唯一编码',
  `active_default_key` TINYINT
    GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 AND `status` = 1 AND `is_default` = 1 THEN 1 ELSE NULL END)
    STORED COMMENT '唯一启用默认仓库约束键',
  `create_by`       VARCHAR(50)  DEFAULT NULL            COMMENT '创建人',
  `update_by`       VARCHAR(50)  DEFAULT NULL            COMMENT '更新人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`        VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`        VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`        DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值1',
  `ext_num2`        DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_sys_warehouse_active_code` (`warehouse_code_active`),
  UNIQUE KEY `uk_warehouse_active_default` (`active_default_key`),
  KEY `idx_warehouse_name` (`warehouse_name`),
  KEY `idx_status` (`status`),
  KEY `idx_warehouse_default_status` (`is_default`, `status`, `is_deleted`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库档案表';

-- =============================================================================
-- 二、加工核心单据
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 3.3.2 biz_process_order 加工单主表（含 V4.1 快照字段 snap_print/snap_finish）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_process_order`;
CREATE TABLE `biz_process_order` (
  `uuid`                  VARCHAR(36)   NOT NULL                COMMENT '单据主键',
  `order_no`              VARCHAR(50)   NOT NULL                COMMENT '加工单号',
  `customer_uuid`         VARCHAR(36)   NOT NULL                COMMENT '关联客户UUID',
  `customer_name`         VARCHAR(100)  NOT NULL                COMMENT '快照冗余客户名',
  `order_date`            DATE          NOT NULL                COMMENT '制单日期',
  `expect_finish_date`    DATE          DEFAULT NULL            COMMENT '预计交货日期',
  `priority`              TINYINT       NOT NULL DEFAULT 1      COMMENT '1普通 2加急 3特急',
  `label_brand`           VARCHAR(100)  DEFAULT NULL            COMMENT '标签品牌',
  `warehouse_uuid`        VARCHAR(36)   DEFAULT NULL            COMMENT '加工仓库',
  `team_group`            VARCHAR(50)   DEFAULT NULL            COMMENT '加工班组',
  `is_invoice`            TINYINT       NOT NULL DEFAULT 2      COMMENT '1开票 2不开票',
  `settle_type`           TINYINT       NOT NULL DEFAULT 2      COMMENT '1次结 2月结，本单结算方式快照/覆盖',
  `settle_day`            TINYINT       DEFAULT NULL            COMMENT '月结对账日，本单快照/覆盖',
  `tax_rate`              DECIMAL(5,2)  DEFAULT 0.00            COMMENT '税率',
  `urgent_fee`            DECIMAL(10,2) DEFAULT 0.00            COMMENT '加急费',
  `pallet_fee`            DECIMAL(10,2) DEFAULT 0.00            COMMENT '托盘费',
  `loading_fee`           DECIMAL(10,2) DEFAULT 0.00            COMMENT '装卸费',
  `freight_fee`           DECIMAL(10,2) DEFAULT 0.00            COMMENT '运费',
  `other_fee`             DECIMAL(10,2) DEFAULT 0.00            COMMENT '其他杂费',
  `process_amount_no_tax` DECIMAL(12,2) DEFAULT 0.00            COMMENT '加工费不含税',
  `process_amount_tax`    DECIMAL(12,2) DEFAULT 0.00            COMMENT '加工费税额',
  `extra_amount_no_tax`   DECIMAL(12,2) DEFAULT 0.00            COMMENT '附加费不含税',
  `extra_amount_tax`      DECIMAL(12,2) DEFAULT 0.00            COMMENT '附加费税额',
  `total_amount_no_tax`   DECIMAL(12,2) DEFAULT 0.00            COMMENT '整单不含税总额',
  `total_amount_tax`      DECIMAL(12,2) DEFAULT 0.00            COMMENT '总税额',
  `total_process_amount`  DECIMAL(12,2) DEFAULT 0.00            COMMENT '加工费合计（取整）',
  `total_extra_amount`    DECIMAL(12,2) DEFAULT 0.00            COMMENT '附加费合计',
  `total_amount`          DECIMAL(12,2) DEFAULT 0.00            COMMENT '整单总金额（取整）',
  `total_original_weight` DECIMAL(10,3) DEFAULT 0.000           COMMENT '原纸总kg',
  `total_original_ton`    DECIMAL(10,3) DEFAULT 0.000           COMMENT '原纸总吨',
  `total_finish_weight`   DECIMAL(10,3) DEFAULT 0.000           COMMENT '成品总kg',
  `total_step_count`      INT           NOT NULL DEFAULT 1      COMMENT '工序总道数',
  `has_extra_step`        TINYINT       NOT NULL DEFAULT 0      COMMENT '0无追加工序 1有追加',
  `actual_total_knife`    INT           DEFAULT 0               COMMENT '全单据实际总刀数',
  `order_status`          TINYINT       NOT NULL DEFAULT 1      COMMENT '0草稿 1待下发 2加工中 3待回录 4已完成 5已结算 6已作废',
  `print_status`          TINYINT       NOT NULL DEFAULT 0      COMMENT '0未打印 1已打印',
  `print_count`           INT           NOT NULL DEFAULT 0      COMMENT '打印次数',
  `last_print_time`       DATETIME      DEFAULT NULL            COMMENT '末次打印时间',
  `last_print_user`       VARCHAR(50)   DEFAULT NULL            COMMENT '打印人',
  `back_record_time`      DATETIME      DEFAULT NULL            COMMENT '回录完成时间',
  `back_record_user`      VARCHAR(50)   DEFAULT NULL            COMMENT '回录操作员',
  `accounting_date`       DATE GENERATED ALWAYS AS (COALESCE(DATE(`back_record_time`), `order_date`)) STORED COMMENT '结算归属日期',
  `void_time`             DATETIME      DEFAULT NULL            COMMENT '作废时间',
  `void_user`             VARCHAR(50)   DEFAULT NULL            COMMENT '作废人',
  `void_reason`           VARCHAR(255)  DEFAULT NULL            COMMENT '作废原因',
  `is_mix_process`        TINYINT       NOT NULL DEFAULT 0      COMMENT '【V4.1】0单一工艺 1混合锯纸+复卷',
  `snap_print`            JSON          DEFAULT NULL            COMMENT '【V4.1】下发快照JSON，根节点必含 schema_version:1.0',
  `snap_finish`           JSON          DEFAULT NULL            COMMENT '【V4.1】完成快照JSON，根节点必含 schema_version:1.0',
  `remark`                VARCHAR(255)  DEFAULT NULL            COMMENT '简短备注',
  `remark_long`           TEXT          DEFAULT NULL            COMMENT '长文本工艺/异常说明',
  `is_deleted`            TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`             VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`             VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`               INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`              VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`              VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`              DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`              DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_customer_uuid` (`customer_uuid`),
  KEY `idx_customer_deleted_ctime` (`customer_uuid`, `is_deleted`, `create_time`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_order_customer_status_accounting` (`customer_uuid`, `order_status`, `accounting_date`, `uuid`),
  KEY `idx_order_report_scope` (`is_deleted`, `order_status`, `accounting_date`, `uuid`),
  KEY `idx_order_date` (`order_date`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加工单主表';

-- -----------------------------------------------------------------------------
-- 3.3.3 biz_original_roll 原纸明细表（单卷维度）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_original_roll`;
CREATE TABLE `biz_original_roll` (
  `uuid`               VARCHAR(36)   NOT NULL                COMMENT '单卷唯一ID',
  `order_uuid`         VARCHAR(36)   NOT NULL                COMMENT '关联加工单',
  `row_sort`           INT           NOT NULL                COMMENT '单据内排序行号',
  `extra_no`           VARCHAR(100)  DEFAULT NULL            COMMENT '客户内部编号',
  `roll_no`            VARCHAR(100)  DEFAULT NULL            COMMENT '来料母卷号，无则空',
  `paper_name`         VARCHAR(100)  NOT NULL                COMMENT '纸张品名',
  `gram_weight`        INT           NOT NULL                COMMENT '来料标称克重 g/㎡',
  `actual_gram_weight` INT           DEFAULT NULL            COMMENT '车间实测实际克重 g/㎡',
  `original_width`     INT           NOT NULL                COMMENT '标称门幅 mm',
  `actual_width`       INT           DEFAULT NULL            COMMENT '实测门幅 mm',
  `original_diameter`  INT           DEFAULT NULL            COMMENT '原卷直径 英寸',
  `core_diameter`      INT           DEFAULT NULL            COMMENT '纸芯直径 英寸',
  `original_length`    INT           DEFAULT NULL            COMMENT '来料长度 米',
  `roll_weight`        DECIMAL(10,3) NOT NULL                COMMENT '标称单件重量 kg',
  `actual_weight`      DECIMAL(10,3) DEFAULT NULL            COMMENT '车间复称实际重量 kg（计费基准）',
  `piece_num`          INT           NOT NULL DEFAULT 1      COMMENT '件数固定默认1',
  `total_weight`       DECIMAL(10,3) NOT NULL DEFAULT 0.000  COMMENT '标称总重=件重*件数',
  `batch_no`           VARCHAR(100)  DEFAULT NULL            COMMENT '来料批次号',
  `damage_desc`        VARCHAR(255)  DEFAULT NULL            COMMENT '破损、水湿文字描述',
  `damage_images`      JSON          DEFAULT NULL            COMMENT '多图片路径数组',
  `process_mode`       TINYINT       NOT NULL DEFAULT 1      COMMENT '1标准加工 2现场定尺 3不加工直发 4仅附加工艺',
  `main_step_type`     TINYINT       DEFAULT NULL            COMMENT '主工艺类型：1锯纸 2复卷（标准加工和现场定尺必填）',
  `roll_status`        TINYINT       NOT NULL DEFAULT 1      COMMENT '1待加工 2加工中 3完成 4直发 5报废',
  `is_checked`         TINYINT       NOT NULL DEFAULT 0      COMMENT '0未复核 1车间线下复核完成',
  `check_user`         VARCHAR(50)   DEFAULT NULL            COMMENT '复核人',
  `check_time`         DATETIME      DEFAULT NULL            COMMENT '复核时间',
  `machine_uuid`       VARCHAR(36)   DEFAULT NULL            COMMENT '加工机台',
  `operator`           VARCHAR(50)   DEFAULT NULL            COMMENT '操作工',
  `process_amount`     DECIMAL(10,2) NOT NULL DEFAULT 0.00  COMMENT '本卷全部工序加工费合计（取整）',
  `total_loss_weight`  DECIMAL(10,3) DEFAULT 0.000           COMMENT '全工序总损耗 kg',
  `total_loss_ratio`   DECIMAL(5,2)  NOT NULL DEFAULT 0.00  COMMENT '损耗率%',
  `customer_name`      VARCHAR(100)  DEFAULT NULL            COMMENT '冗余客户名称',
  `order_no`           VARCHAR(50)   DEFAULT NULL            COMMENT '冗余加工单号',
  `remark`             VARCHAR(255)  DEFAULT NULL            COMMENT '单卷备注',
  `is_deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`          VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`          VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`            INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`           VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`           VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`           DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`           DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  KEY `idx_order_uuid` (`order_uuid`),
  KEY `idx_roll_no` (`roll_no`),
  KEY `idx_roll_status` (`roll_status`),
  KEY `idx_process_mode` (`process_mode`),
  KEY `idx_main_step_type` (`main_step_type`),
  KEY `idx_row_sort` (`order_uuid`, `row_sort`),
  KEY `idx_original_roll_machine_uuid` (`machine_uuid`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_original_roll_paper_candidate` (`is_deleted`, `paper_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原纸明细表（单卷维度）';

-- -----------------------------------------------------------------------------
-- 3.3.4 biz_process_step 工序明细表（工艺唯一来源）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_machine_process_capability`;
DROP TABLE IF EXISTS `sys_process_catalog_billing_mode`;
DROP TABLE IF EXISTS `sys_process_catalog_unit`;
DROP TABLE IF EXISTS `sys_process_catalog`;
CREATE TABLE `sys_process_catalog` (
  `uuid` VARCHAR(36) NOT NULL,
  `step_type` TINYINT NOT NULL COMMENT 'Legacy-compatible process type',
  `process_code` VARCHAR(50) NOT NULL,
  `process_name` VARCHAR(80) NOT NULL,
  `process_category` VARCHAR(20) NOT NULL,
  `pricing_strategy` VARCHAR(30) NOT NULL,
  `produces_inventory_output` TINYINT NOT NULL DEFAULT 0,
  `allows_loss_recording` TINYINT NOT NULL DEFAULT 0,
  `allows_main_process` TINYINT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `sort_no` INT NOT NULL DEFAULT 100,
  `built_in` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_process_catalog_step_type` (`step_type`),
  UNIQUE KEY `uk_process_catalog_code` (`process_code`),
  KEY `idx_process_catalog_active` (`status`,`is_deleted`,`sort_no`),
  CONSTRAINT `chk_process_catalog_category` CHECK (`process_category` IN
    ('PRODUCTION','SERVICE','QUALITY','PACKAGING','LOGISTICS')),
  CONSTRAINT `chk_process_catalog_strategy` CHECK (`pricing_strategy` IN
    ('SAW_KNIFE','REWIND_WEIGHT','SERVICE_QUANTITY')),
  CONSTRAINT `chk_process_catalog_flags` CHECK (
    `produces_inventory_output` IN (0,1) AND `allows_loss_recording` IN (0,1)
    AND `allows_main_process` IN (0,1) AND `status` IN (0,1) AND `built_in` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Process capability catalog';

CREATE TABLE `sys_process_catalog_unit` (
  `catalog_uuid` VARCHAR(36) NOT NULL,
  `unit_code` VARCHAR(16) NOT NULL,
  `unit_name` VARCHAR(30) NOT NULL,
  `is_default` TINYINT NOT NULL DEFAULT 0,
  `default_catalog_uuid` VARCHAR(36) GENERATED ALWAYS AS
    (CASE WHEN `is_default`=1 THEN `catalog_uuid` ELSE NULL END) STORED,
  `sort_no` INT NOT NULL DEFAULT 100,
  PRIMARY KEY (`catalog_uuid`,`unit_code`),
  UNIQUE KEY `uk_process_catalog_default_unit` (`default_catalog_uuid`),
  CONSTRAINT `fk_process_catalog_unit_catalog` FOREIGN KEY (`catalog_uuid`)
    REFERENCES `sys_process_catalog` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_process_catalog_unit_default` CHECK (`is_default` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Allowed process measurement units';

CREATE TABLE `sys_process_catalog_billing_mode` (
  `catalog_uuid` VARCHAR(36) NOT NULL,
  `billing_mode` TINYINT NOT NULL,
  `sort_no` INT NOT NULL DEFAULT 100,
  PRIMARY KEY (`catalog_uuid`,`billing_mode`),
  CONSTRAINT `fk_process_catalog_billing_catalog` FOREIGN KEY (`catalog_uuid`)
    REFERENCES `sys_process_catalog` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_process_catalog_billing_mode` CHECK (`billing_mode` IN (1,2,3,4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Allowed process billing modes';

CREATE TABLE `sys_machine_process_capability` (
  `uuid` VARCHAR(36) NOT NULL,
  `machine_uuid` VARCHAR(36) NOT NULL,
  `catalog_uuid` VARCHAR(36) NOT NULL,
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '当前工艺默认资源',
  `priority` INT NOT NULL DEFAULT 100 COMMENT '候选排序，越小越优先',
  `min_width` INT DEFAULT NULL COMMENT '最小适用门幅mm',
  `max_width` INT DEFAULT NULL COMMENT '最大适用门幅mm',
  `max_roll_weight` DECIMAL(12,3) DEFAULT NULL COMMENT '最大卷重kg',
  `max_diameter` INT DEFAULT NULL COMMENT '最大卷径mm',
  `remark` VARCHAR(255) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  `active_machine_catalog_key` VARCHAR(80) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted`=0 THEN CONCAT(`machine_uuid`,':',`catalog_uuid`) ELSE NULL END) STORED,
  `active_default_catalog_key` VARCHAR(36) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted`=0 AND `is_default`=1 THEN `catalog_uuid` ELSE NULL END) STORED,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_machine_capability_active` (`active_machine_catalog_key`),
  UNIQUE KEY `uk_machine_capability_default` (`active_default_catalog_key`),
  KEY `idx_machine_capability_machine` (`machine_uuid`,`is_deleted`),
  KEY `idx_machine_capability_catalog` (`catalog_uuid`,`is_deleted`,`priority`),
  CONSTRAINT `fk_machine_capability_machine` FOREIGN KEY (`machine_uuid`) REFERENCES `sys_machine` (`uuid`) ON DELETE RESTRICT,
  CONSTRAINT `fk_machine_capability_catalog` FOREIGN KEY (`catalog_uuid`) REFERENCES `sys_process_catalog` (`uuid`) ON DELETE RESTRICT,
  CONSTRAINT `chk_machine_capability_default` CHECK (`is_default` IN (0,1)),
  CONSTRAINT `chk_machine_capability_priority` CHECK (`priority` BETWEEN 1 AND 9999),
  CONSTRAINT `chk_machine_capability_width` CHECK ((`min_width` IS NULL OR `min_width`>0) AND (`max_width` IS NULL OR `max_width`>0) AND (`min_width` IS NULL OR `max_width` IS NULL OR `min_width`<=`max_width`)),
  CONSTRAINT `chk_machine_capability_limits` CHECK ((`max_roll_weight` IS NULL OR `max_roll_weight`>0) AND (`max_diameter` IS NULL OR `max_diameter`>0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机台/工位支持的工艺能力';

INSERT INTO `sys_process_catalog`
(`uuid`,`step_type`,`process_code`,`process_name`,`process_category`,`pricing_strategy`,
 `produces_inventory_output`,`allows_loss_recording`,`allows_main_process`,`status`,`sort_no`,`built_in`) VALUES
('process-catalog-saw',1,'SAW','锯纸','PRODUCTION','SAW_KNIFE',1,1,1,1,10,1),
('process-catalog-rewind',2,'REWIND','复卷','PRODUCTION','REWIND_WEIGHT',1,1,1,1,20,1),
('process-catalog-strip',3,'STRIP_SORT','剥损整理','SERVICE','SERVICE_QUANTITY',0,1,0,1,30,1),
('process-catalog-repack',4,'REPACK','重新包装','PACKAGING','SERVICE_QUANTITY',0,0,0,1,40,1);

INSERT INTO `sys_process_catalog_unit`
(`catalog_uuid`,`unit_code`,`unit_name`,`is_default`,`sort_no`) VALUES
('process-catalog-saw','KNIFE','刀',1,10),
('process-catalog-rewind','TON','吨',1,10),
('process-catalog-strip','PIECE','件',1,10),
('process-catalog-strip','TON','吨',0,20),
('process-catalog-repack','PIECE','件',1,10),
('process-catalog-repack','TON','吨',0,20);

INSERT INTO `sys_process_catalog_billing_mode`
(`catalog_uuid`,`billing_mode`,`sort_no`) VALUES
('process-catalog-saw',1,10),('process-catalog-saw',2,20),
('process-catalog-saw',3,30),('process-catalog-saw',4,40),
('process-catalog-rewind',1,10),('process-catalog-rewind',2,20),
('process-catalog-rewind',3,30),('process-catalog-rewind',4,40),
('process-catalog-strip',1,10),('process-catalog-strip',3,30),('process-catalog-strip',4,40),
('process-catalog-repack',1,10),('process-catalog-repack',3,30),('process-catalog-repack',4,40);

DROP TABLE IF EXISTS `sys_customer_process_price`;
CREATE TABLE `sys_customer_process_price` (
  `uuid` VARCHAR(36) NOT NULL,
  `customer_uuid` VARCHAR(36) NOT NULL,
  `catalog_uuid` VARCHAR(36) NOT NULL,
  `billing_basis` VARCHAR(12) NOT NULL COMMENT 'PIECE按件 TON按吨 FIXED固定金额',
  `price` DECIMAL(12,2) NOT NULL,
  `is_default` TINYINT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  `active_price_key` VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN `is_deleted`=0 THEN CONCAT(`customer_uuid`,':',`catalog_uuid`,':',`billing_basis`) ELSE NULL END) STORED,
  `active_default_key` VARCHAR(80) GENERATED ALWAYS AS (CASE WHEN `is_deleted`=0 AND `is_default`=1 THEN CONCAT(`customer_uuid`,':',`catalog_uuid`) ELSE NULL END) STORED,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_customer_process_price_active` (`active_price_key`),
  UNIQUE KEY `uk_customer_process_price_default` (`active_default_key`),
  KEY `idx_customer_process_price_customer` (`customer_uuid`,`is_deleted`),
  KEY `idx_customer_process_price_catalog` (`catalog_uuid`,`is_deleted`),
  CONSTRAINT `fk_customer_process_price_customer` FOREIGN KEY (`customer_uuid`) REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT,
  CONSTRAINT `fk_customer_process_price_catalog` FOREIGN KEY (`catalog_uuid`) REFERENCES `sys_process_catalog` (`uuid`) ON DELETE RESTRICT,
  CONSTRAINT `chk_customer_process_price_basis` CHECK (`billing_basis` IN ('PIECE','TON','FIXED')),
  CONSTRAINT `chk_customer_process_price_value` CHECK (`price` > 0),
  CONSTRAINT `chk_customer_process_price_default` CHECK (`is_default` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户服务工艺价格方案';

DROP TABLE IF EXISTS `biz_process_step`;
CREATE TABLE `biz_process_step` (
  `uuid`           VARCHAR(36)   NOT NULL                COMMENT '工序ID',
  `order_uuid`     VARCHAR(36)   NOT NULL                COMMENT '关联加工单',
  `original_uuid`  VARCHAR(36)   NOT NULL                COMMENT '关联对应原纸单卷',
  `input_type`     TINYINT       NOT NULL DEFAULT 1      COMMENT '输入来源：1原纸 2上一阶段产出',
  `input_output_uuid` VARCHAR(36) DEFAULT NULL           COMMENT '输入阶段产出UUID，input_type=2时使用',
  `stage_level`    INT           NOT NULL DEFAULT 1      COMMENT '工艺阶段层级：1第一道 2第二道',
  `parent_step_uuid` VARCHAR(36) DEFAULT NULL            COMMENT '上一阶段工序UUID',
  `step_sort`      INT           NOT NULL DEFAULT 1      COMMENT '工序排序号',
  `step_type`      TINYINT       NOT NULL                COMMENT '1锯纸 2复卷 3剥损整理 4重新包装',
  `step_name`      VARCHAR(50)   DEFAULT NULL            COMMENT '工序自定义名称',
  `machine_uuid`   VARCHAR(36)   DEFAULT NULL            COMMENT '工序加工机台',
  `machine_name_snap` VARCHAR(100) DEFAULT NULL           COMMENT '工序机台名称快照',
  `is_main`        TINYINT       NOT NULL DEFAULT 1      COMMENT '1本卷主工艺 0车间追加工序',
  `knife_count`    INT           DEFAULT 0               COMMENT '锯纸专用：实际加工刀数',
    `process_weight` DECIMAL(10,3) DEFAULT NULL            COMMENT '复卷专用：加工吨位',
    `billing_basis` VARCHAR(16) DEFAULT NULL                COMMENT '服务计费基准 TON按吨 PIECE按件',
    `service_quantity` DECIMAL(12,3) DEFAULT NULL           COMMENT '整理或包装服务数量',
    `unit_price`     DECIMAL(10,2) DEFAULT NULL            COMMENT '本工序单价（元/刀 / 元/吨）',
    `billing_unit_price` DECIMAL(12,4) DEFAULT NULL        COMMENT '人工核定单价，为空时沿用标准单价',
  `step_amount`    DECIMAL(10,2) DEFAULT 0.00            COMMENT '本工序加工费（取整）',
  `billing_mode`   TINYINT       NOT NULL DEFAULT 1      COMMENT '1标准计价 2指定数量 3固定金额 4免收',
  `standard_quantity` DECIMAL(12,3) DEFAULT NULL         COMMENT '优惠前标准计费数量',
  `billing_quantity` DECIMAL(12,3) DEFAULT NULL          COMMENT '最终计费数量',
  `billing_amount` DECIMAL(12,2) DEFAULT NULL            COMMENT '固定金额模式最终金额',
  `standard_step_amount` DECIMAL(12,2) DEFAULT NULL      COMMENT '优惠前标准工序金额',
  `pricing_adjustment_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '最终金额减标准金额',
  `pricing_adjustment_reason` VARCHAR(255) DEFAULT NULL  COMMENT '计价调整原因',
  `pricing_adjusted_by` VARCHAR(50) DEFAULT NULL         COMMENT '计价调整操作人',
    `pricing_adjusted_at` DATETIME DEFAULT NULL            COMMENT '计价调整时间',
    `pricing_adjustment_batch_id` VARCHAR(64) DEFAULT NULL COMMENT '批量计价操作标识',
  `width_difference_policy` VARCHAR(16) DEFAULT NULL       COMMENT 'LOSS计损耗 ALLOCATE分摊 REMAINDER留余料',
  `planned_loss_width` INT DEFAULT NULL                    COMMENT '计划非库存损耗门幅 mm',
  `planned_loss_weight` DECIMAL(10,3) DEFAULT NULL         COMMENT '计划非库存损耗重量 kg',
  `loss_weight`    DECIMAL(10,3) DEFAULT 0.000           COMMENT '本工序产生损耗重量 kg',
  `operator`       VARCHAR(50)   DEFAULT NULL            COMMENT '本工序操作工',
  `remark`         VARCHAR(255)  DEFAULT NULL            COMMENT '工序备注、异常说明',
  `is_deleted`     TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `active_main_original_uuid` VARCHAR(36)
    GENERATED ALWAYS AS (
      CASE WHEN `is_main` = 1 AND `is_deleted` = 0 THEN `original_uuid` ELSE NULL END
    ) STORED COMMENT '有效主工艺母卷UUID，用于数据库唯一约束',
  `create_by`      VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`      VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`        INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`       VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`       VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`       DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`       DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  KEY `idx_order_uuid` (`order_uuid`),
  KEY `idx_original_uuid` (`original_uuid`),
  KEY `idx_input_output_uuid` (`input_output_uuid`),
  KEY `idx_parent_step_uuid` (`parent_step_uuid`),
  KEY `idx_process_step_machine_uuid` (`machine_uuid`),
  KEY `idx_step_type` (`step_type`),
  KEY `idx_is_deleted` (`is_deleted`),
  UNIQUE KEY `uk_roll_main_step` (`active_main_original_uuid`),
    CONSTRAINT `chk_process_step_billing_mode` CHECK (`billing_mode` IN (1,2,3,4)),
    CONSTRAINT `fk_process_step_catalog_type` FOREIGN KEY (`step_type`)
      REFERENCES `sys_process_catalog` (`step_type`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `chk_process_step_service_quantity` CHECK (`service_quantity` IS NULL OR `service_quantity` > 0),
    CONSTRAINT `chk_process_step_billing_unit_price` CHECK (`billing_unit_price` IS NULL OR `billing_unit_price` > 0),
  CONSTRAINT `chk_process_step_width_policy` CHECK (`width_difference_policy` IS NULL OR `width_difference_policy` IN ('LOSS','ALLOCATE','REMAINDER')),
  CONSTRAINT `chk_process_step_planned_loss` CHECK ((`planned_loss_width` IS NULL OR `planned_loss_width` >= 0) AND (`planned_loss_weight` IS NULL OR `planned_loss_weight` >= 0)),
  CONSTRAINT `chk_process_step_pricing_nonnegative` CHECK (
    (`standard_quantity` IS NULL OR `standard_quantity` >= 0)
    AND (`billing_quantity` IS NULL OR `billing_quantity` > 0)
      AND (`billing_amount` IS NULL OR `billing_amount` >= 0)
      AND (`billing_unit_price` IS NULL OR `billing_unit_price` > 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序明细表（工艺唯一来源）';

-- -----------------------------------------------------------------------------
-- 3.3.4.1 biz_process_stage_output 工艺阶段产出表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_process_stage_output`;
CREATE TABLE `biz_process_stage_output` (
  `uuid`                 VARCHAR(36)   NOT NULL                COMMENT '阶段产出主键',
  `order_uuid`           VARCHAR(36)   NOT NULL                COMMENT '关联加工单',
  `original_uuid`        VARCHAR(36)   NOT NULL                COMMENT '根母卷UUID',
  `step_uuid`            VARCHAR(36)   NOT NULL                COMMENT '产出所属工序',
  `parent_output_uuid`   VARCHAR(36)   DEFAULT NULL            COMMENT '上一阶段产出UUID',
  `stage_level`          INT           NOT NULL DEFAULT 1      COMMENT '工艺阶段层级',
  `output_sort`          INT           NOT NULL DEFAULT 1      COMMENT '阶段内产出排序',
  `output_type`          TINYINT       NOT NULL DEFAULT 2      COMMENT '1中间产出 2最终产出',
  `output_status`        TINYINT       NOT NULL DEFAULT 1      COMMENT '1计划 2已被下道消耗 3已生成成品卷 4作废',
  `output_no`            VARCHAR(50)   DEFAULT NULL            COMMENT '阶段产出临时编号',
  `finish_roll_uuid`     VARCHAR(36)   DEFAULT NULL            COMMENT '最终产出生成的成品卷UUID',
  `paper_name`           VARCHAR(100)  DEFAULT NULL            COMMENT '产出品名',
  `gram_weight`          INT           DEFAULT NULL            COMMENT '产出克重 g/㎡',
  `finish_width`         INT           DEFAULT NULL            COMMENT '产出门幅 mm',
  `finish_diameter`      INT           DEFAULT NULL            COMMENT '产出直径 英寸',
  `finish_core_diameter` INT           DEFAULT NULL            COMMENT '产出纸芯 英寸',
  `estimate_weight`      DECIMAL(10,3) DEFAULT NULL            COMMENT '预估重量 kg',
  `actual_weight`        DECIMAL(10,3) DEFAULT NULL            COMMENT '实际重量 kg',
  `source_step_type`     TINYINT       DEFAULT NULL            COMMENT '产出来源工艺：1锯纸 2复卷',
  `source_summary`       VARCHAR(255)  DEFAULT NULL            COMMENT '来源摘要',
  `remark`               VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `is_deleted`           TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`            VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`            VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`              INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`             VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`             VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`             DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`             DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  KEY `idx_order_uuid` (`order_uuid`),
  KEY `idx_original_uuid` (`original_uuid`),
  KEY `idx_step_uuid` (`step_uuid`),
  KEY `idx_parent_output_uuid` (`parent_output_uuid`),
  KEY `idx_finish_roll_uuid` (`finish_roll_uuid`),
  KEY `idx_output_type` (`output_type`),
  KEY `idx_output_status` (`output_status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺阶段产出表';

-- -----------------------------------------------------------------------------
-- 3.3.4.2 biz_process_stage_input_rel 工艺阶段输入关联表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_process_stage_input_rel`;
CREATE TABLE `biz_process_stage_input_rel` (
  `uuid`              VARCHAR(36)   NOT NULL                COMMENT '阶段输入关联主键',
  `order_uuid`        VARCHAR(36)   NOT NULL                COMMENT '关联加工单',
  `original_uuid`     VARCHAR(36)   NOT NULL                COMMENT '根母卷UUID',
  `step_uuid`         VARCHAR(36)   NOT NULL                COMMENT '消费这些阶段产出的工序UUID',
  `input_output_uuid` VARCHAR(36)   NOT NULL                COMMENT '被消费的阶段产出UUID',
  `source_step_uuid`  VARCHAR(36)   DEFAULT NULL            COMMENT '阶段产出来源工序UUID',
  `input_sort`        INT           NOT NULL DEFAULT 1      COMMENT '本工序输入顺序',
  `stage_level`       INT           NOT NULL DEFAULT 1      COMMENT '消费工序所在阶段层级',
  `is_deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`         VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`         VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`           INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`          VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`          VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`          DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`          DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  KEY `idx_order_uuid` (`order_uuid`),
  KEY `idx_original_uuid` (`original_uuid`),
  KEY `idx_step_uuid` (`step_uuid`),
  KEY `idx_input_output_uuid` (`input_output_uuid`),
  KEY `idx_source_step_uuid` (`source_step_uuid`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺阶段输入关联表';

-- -----------------------------------------------------------------------------
-- 3.3.5 biz_finish_roll 成品明细表（全局唯一卷号）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_finish_roll`;
CREATE TABLE `biz_finish_roll` (
  `uuid`                 VARCHAR(36)   NOT NULL                COMMENT '成品主键',
  `order_uuid`           VARCHAR(36)   NOT NULL                COMMENT '关联加工单',
  `row_sort`             INT           NOT NULL                COMMENT '单据内排序',
  `finish_roll_no`       VARCHAR(20)   DEFAULT NULL            COMMENT '成品全局唯一编号：1大写字母+6位数字',
  `finish_inner_no`      VARCHAR(100)  DEFAULT NULL            COMMENT '内部自定义编号',
  `roll_no_status`       TINYINT       NOT NULL DEFAULT 1      COMMENT '1预生成 2已使用 3作废',
  `is_spare`             TINYINT       NOT NULL DEFAULT 0      COMMENT '0正式成品号 1备用冗余卷号',
  `paper_name`           VARCHAR(100)  NOT NULL                COMMENT '成品品名',
  `customer_paper_name`  VARCHAR(100)  DEFAULT NULL            COMMENT '客户对外品名',
  `gram_weight`          INT           NOT NULL                COMMENT '成品克重 g/㎡',
  `customer_gram_weight` INT           DEFAULT NULL            COMMENT '客户要求标注克重 g/㎡',
  `finish_width`         INT           NOT NULL                COMMENT '成品门幅 mm',
  `customer_finish_width` INT          DEFAULT NULL            COMMENT '客户销售门幅 mm',
  `customer_display_weight` DECIMAL(12,3) DEFAULT NULL         COMMENT '客户对外显示重量 kg',
  `customer_spec_override_reason` VARCHAR(255) DEFAULT NULL    COMMENT '客户规格改写原因',
  `customer_spec_override_by` VARCHAR(50) DEFAULT NULL         COMMENT '客户规格改写人',
  `customer_spec_override_at` DATETIME DEFAULT NULL            COMMENT '客户规格改写时间',
  `finish_diameter`      INT           DEFAULT NULL            COMMENT '成品直径 英寸',
  `finish_core_diameter` INT           DEFAULT NULL            COMMENT '成品纸芯直径 英寸',
  `source_type`          TINYINT       NOT NULL DEFAULT 1      COMMENT '1加工产出 2原纸直发(沿用母卷号,三不约束) 3仅附加工艺产出',
  `estimate_weight_snap` DECIMAL(10,3) DEFAULT NULL            COMMENT '打印下发时预估重量快照 kg',
  `estimate_weight`      DECIMAL(10,3) DEFAULT NULL            COMMENT '当前系统理论预估重量 kg',
  `actual_weight`        DECIMAL(10,3) DEFAULT NULL            COMMENT '车间实际成品重量 kg',
  `remaining_weight`     DECIMAL(10,3) DEFAULT NULL            COMMENT '剩余可出库重量 kg，NULL按actual_weight兼容旧数据',
  `diameter_ratio`       DECIMAL(5,2)  DEFAULT NULL            COMMENT '直径分卷重量占比%',
  `trim_width_share`     INT           DEFAULT NULL            COMMENT '分摊修边宽度 mm',
  `trim_weight_share`    DECIMAL(10,3) DEFAULT NULL            COMMENT '分摊修边重量 kg',
  `is_weight_adjust`     TINYINT       NOT NULL DEFAULT 0      COMMENT '0未人工调重 1人工修改重量',
  `weight_adjust_reason` VARCHAR(255)  DEFAULT NULL            COMMENT '调重原因',
  `weight_diff`          DECIMAL(10,3) DEFAULT 0.000           COMMENT '重量调整差值 kg',
  `is_manual_edit`       TINYINT       NOT NULL DEFAULT 0      COMMENT '是否人工修改规格尺寸',
  `is_remain`            TINYINT       NOT NULL DEFAULT 0      COMMENT '0正品 1边角余料',
  `is_abnormal`          TINYINT       NOT NULL DEFAULT 0      COMMENT '是否异常次品',
  `abnormal_type`        VARCHAR(50)   DEFAULT NULL            COMMENT '异常类型描述',
  `scrap_weight`         DECIMAL(10,3) DEFAULT 0.000           COMMENT '报废重量 kg',
  `quality_status`       TINYINT       DEFAULT 1               COMMENT '1待检 2合格 3不合格 4让步接收',
  `finish_status`        TINYINT       NOT NULL DEFAULT 1      COMMENT '1待入库 2已入库 3已出库 4报废',
  `stock_in_time`        DATETIME      DEFAULT NULL            COMMENT '首次正式入库时间',
  `warehouse_uuid`       VARCHAR(36)   DEFAULT NULL            COMMENT '存放仓库',
  `original_roll_nos`    TEXT          DEFAULT NULL            COMMENT '来源母卷号拼接，用于溯源',
  `actual_remark`        VARCHAR(255)  DEFAULT NULL            COMMENT '车间手写备注',
  `remark`               VARCHAR(255)  DEFAULT NULL            COMMENT '系统备注',
  `is_deleted`           TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`            VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`            VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`              INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`             VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`             VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`             DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`             DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_finish_roll_no` (`finish_roll_no`),
  KEY `idx_order_uuid` (`order_uuid`),
  KEY `idx_finish_status` (`finish_status`),
  KEY `idx_finish_inventory_filter` (`finish_status`, `is_deleted`, `warehouse_uuid`, `stock_in_time`),
  KEY `idx_report_inventory_scope` (`finish_status`, `is_deleted`, `stock_in_time`, `order_uuid`),
  KEY `idx_roll_no_status` (`roll_no_status`),
  KEY `idx_source_type` (`source_type`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成品明细表（全局唯一卷号）';

-- 说明: finish_roll_no 全局唯一索引 uk_finish_roll_no 兜底防重；
--       source_type=2 直发记录沿用母卷号且不占字母流水，唯一索引允许其与母卷号同值（直发记录全局也唯一）。

-- -----------------------------------------------------------------------------
-- 3.3.6 biz_process_param 复卷分层/直径拆分工艺参数表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_process_param`;
CREATE TABLE `biz_process_param` (
  `uuid`          VARCHAR(36)   NOT NULL                COMMENT '参数行主键',
  `order_uuid`    VARCHAR(36)   NOT NULL                COMMENT '关联加工单',
  `original_uuid` VARCHAR(36)   NOT NULL                COMMENT '关联原纸单卷',
  `step_uuid`     VARCHAR(36)   DEFAULT NULL            COMMENT '关联工序（复卷工序）',
  `param_mode`    TINYINT       NOT NULL                COMMENT '1改门幅 2改直径 3改直径+门幅 4分层 5合并复卷',
  `layer_sort`    INT           NOT NULL DEFAULT 1      COMMENT '分层序号（内外层场景）',
  `out_diameter`  INT           DEFAULT NULL            COMMENT '本层/本件外径 英寸',
  `core_diameter` INT           DEFAULT NULL            COMMENT '本层纸芯直径 英寸',
  `layer_width`   INT           DEFAULT NULL            COMMENT '本层/本件门幅 mm',
  `area_value`    DECIMAL(14,3) DEFAULT NULL            COMMENT '计算横截面积 mm²',
  `area_ratio`    DECIMAL(10,3) DEFAULT NULL            COMMENT '历史字段：预估重量kg，不再按百分比展示',
  `split_ratio`   DECIMAL(5,2)  DEFAULT NULL            COMMENT '合并复卷自定义分摊比例%',
  `param_json`    JSON          DEFAULT NULL            COMMENT '复杂分层参数原始快照',
  `remark`        VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `is_deleted`    TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`     VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`     VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`       INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`      VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`      VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`      DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`      DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  KEY `idx_order_uuid` (`order_uuid`),
  KEY `idx_original_uuid` (`original_uuid`),
  KEY `idx_step_uuid` (`step_uuid`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复卷分层/直径拆分工艺参数表';

-- -----------------------------------------------------------------------------
-- 3.3.7 biz_finish_original_rel 成品-原纸关联表（合并复卷多对多）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_finish_original_rel`;
CREATE TABLE `biz_finish_original_rel` (
  `uuid`          VARCHAR(36)   NOT NULL                COMMENT '关联行主键',
  `finish_uuid`   VARCHAR(36)   NOT NULL                COMMENT '关联成品 biz_finish_roll.uuid',
  `original_uuid` VARCHAR(36)   NOT NULL                COMMENT '关联原纸 biz_original_roll.uuid',
  `order_uuid`    VARCHAR(36)   NOT NULL                COMMENT '关联加工单',
  `share_ratio`   DECIMAL(5,2)  DEFAULT NULL            COMMENT '本原卷对本成品的重量分摊比例%',
  `share_weight`  DECIMAL(10,3) DEFAULT NULL            COMMENT '本原卷分摊到本成品的重量 kg',
  `remark`        VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `is_deleted`    TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`     VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`     VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`       INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`      VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`      VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`      DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`      DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  KEY `idx_finish_uuid` (`finish_uuid`),
  KEY `idx_original_uuid` (`original_uuid`),
  KEY `idx_order_uuid` (`order_uuid`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成品-原纸关联表（合并复卷多对多）';

-- =============================================================================
-- 三、出库模块
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 3.3.8 biz_delivery_order 出库单主表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_delivery_order`;
CREATE TABLE `biz_delivery_order` (
  `uuid`                VARCHAR(36)   NOT NULL                COMMENT '出库单主键',
  `delivery_no`         VARCHAR(50)   NOT NULL                COMMENT '出库单号',
  `customer_uuid`       VARCHAR(36)   NOT NULL                COMMENT '关联客户',
  `customer_name`       VARCHAR(100)  NOT NULL                COMMENT '快照冗余客户名',
  `warehouse_uuid`      VARCHAR(36)   DEFAULT NULL            COMMENT '出库仓库',
  `warehouse_name`      VARCHAR(100)  DEFAULT NULL            COMMENT '出库仓库名称快照',
  `delivery_date`       DATE          NOT NULL                COMMENT '出库日期',
  `total_count`         INT           NOT NULL DEFAULT 0      COMMENT '出库成品件数',
  `total_weight`        DECIMAL(12,3) DEFAULT 0.000           COMMENT '出库总重量 kg',
  `picker_name`         VARCHAR(50)   DEFAULT NULL            COMMENT '提货人',
  `car_no`              VARCHAR(50)   DEFAULT NULL            COMMENT '车牌号',
  `container_no`        VARCHAR(50)   DEFAULT NULL            COMMENT '柜号',
  `sign_user`           VARCHAR(50)   DEFAULT NULL            COMMENT '签收人',
  `sign_time`           DATETIME      DEFAULT NULL            COMMENT '签收时间',
  `settle_block_action` TINYINT       NOT NULL DEFAULT 0      COMMENT '现结拦截结果 0无 1警告放行 2拦截',
  `delivery_status`     TINYINT       NOT NULL DEFAULT 1      COMMENT '1待出库 2已出库签收 3已作废',
  `void_reason`         VARCHAR(255)  DEFAULT NULL            COMMENT '作废原因',
  `void_by`             VARCHAR(50)   DEFAULT NULL            COMMENT '作废操作人',
  `void_time`           DATETIME      DEFAULT NULL            COMMENT '作废时间',
  `snap_delivery`        JSON          DEFAULT NULL            COMMENT '出库确认快照JSON',
  `snap_delivery_time`   DATETIME      DEFAULT NULL            COMMENT '出库确认快照时间',
  `remark`              VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `is_deleted`          TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`           VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`           VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`             INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`            VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`            VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`            DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`            DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_delivery_no` (`delivery_no`),
  KEY `idx_customer_uuid` (`customer_uuid`),
  KEY `idx_delivery_status` (`delivery_status`),
  KEY `idx_delivery_date` (`delivery_date`),
  KEY `idx_report_delivery_scope` (`is_deleted`, `delivery_status`, `delivery_date`, `customer_uuid`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单主表';

-- -----------------------------------------------------------------------------
-- 3.3.9 biz_delivery_detail 出库成品明细表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_delivery_detail`;
CREATE TABLE `biz_delivery_detail` (
  `uuid`           VARCHAR(36)   NOT NULL                COMMENT '明细主键',
  `delivery_uuid`  VARCHAR(36)   NOT NULL                COMMENT '关联出库单',
  `finish_uuid`    VARCHAR(36)   NOT NULL                COMMENT '关联成品 biz_finish_roll.uuid（含直发source_type=2）',
  `order_uuid`     VARCHAR(36)   NOT NULL                COMMENT '来源加工单',
  `finish_roll_no` VARCHAR(20)   DEFAULT NULL            COMMENT '冗余成品卷号',
  `paper_name`     VARCHAR(100)  DEFAULT NULL            COMMENT '冗余品名',
  `out_weight`     DECIMAL(10,3) NOT NULL                COMMENT '本件出库重量 kg',
  `stock_lock_status` TINYINT    NOT NULL DEFAULT 1      COMMENT '库存占用状态：1待出库占用 0历史明细不占用',
  `remark`         VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `is_deleted`     TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `finish_uuid_active` VARCHAR(36)
    GENERATED ALWAYS AS (
      CASE WHEN `is_deleted` = 0 AND `stock_lock_status` = 1 THEN NULLIF(TRIM(`finish_uuid`), '') ELSE NULL END
    ) STORED COMMENT 'active stock lock finish roll',
  `create_by`      VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`      VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`        INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`       VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`       VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`       DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`       DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_biz_delivery_detail_active_finish` (`finish_uuid_active`),
  KEY `idx_delivery_uuid` (`delivery_uuid`),
  KEY `idx_finish_uuid` (`finish_uuid`),
  KEY `idx_order_uuid` (`order_uuid`),
  KEY `idx_stock_lock_status` (`stock_lock_status`),
  KEY `idx_report_delivery_detail` (`is_deleted`, `delivery_uuid`, `stock_lock_status`, `finish_uuid`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库成品明细表';

-- =============================================================================
-- 四、结算收款模块
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 3.3.10 biz_settle_order 结算单主表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_settle_collection_reminder`;
DROP TABLE IF EXISTS `biz_settle_order`;
CREATE TABLE `biz_settle_order` (
  `uuid`              VARCHAR(36)   NOT NULL                COMMENT '结算单主键',
  `settle_no`         VARCHAR(50)   NOT NULL                COMMENT '结算单号',
  `customer_uuid`     VARCHAR(36)   NOT NULL                COMMENT '关联客户',
  `customer_name`     VARCHAR(100)  NOT NULL                COMMENT '快照冗余客户名',
  `request_id`        VARCHAR(64)   DEFAULT NULL            COMMENT '客户端幂等请求号',
  `quote_version`     VARCHAR(32)   DEFAULT NULL            COMMENT '创建时报价算法版本',
  `quote_hash`        CHAR(64)      DEFAULT NULL            COMMENT '创建时报价SHA-256',
  `settle_type`       TINYINT       NOT NULL DEFAULT 1      COMMENT '1按单 2按月批量 3勾选合并',
  `settle_date`       DATE          NOT NULL                COMMENT '结算日期',
  `due_date`          DATE          DEFAULT NULL            COMMENT '付款到期日',
  `period_start`      DATE          DEFAULT NULL            COMMENT '月结账期起',
  `period_end`        DATE          DEFAULT NULL            COMMENT '月结账期止',
  `saw_amount`        DECIMAL(12,2) DEFAULT 0.00            COMMENT '锯纸加工费合计',
  `rewind_amount`     DECIMAL(12,2) DEFAULT 0.00            COMMENT '复卷加工费合计',
  `service_amount`    DECIMAL(12,2) NOT NULL DEFAULT 0.00  COMMENT '整理包装等服务工序费',
  `extra_amount`      DECIMAL(12,2) DEFAULT 0.00            COMMENT '附加费合计',
  `amount_no_tax`     DECIMAL(12,2) DEFAULT 0.00            COMMENT '不含税金额',
  `tax_amount`        DECIMAL(12,2) DEFAULT 0.00            COMMENT '税额',
  `total_amount`      DECIMAL(12,2) NOT NULL DEFAULT 0.00  COMMENT '应收总金额（取整）',
  `received_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0.00  COMMENT '已结清金额',
  `cash_received_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '实际到账金额',
  `scrap_offset_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '废纸抵扣金额',
  `discount_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0.00  COMMENT '优惠及尾差核销金额',
  `unreceived_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00  COMMENT '待收金额',
  `reminder_count`    INT           NOT NULL DEFAULT 0     COMMENT '催收提醒次数',
  `last_reminder_time` DATETIME     DEFAULT NULL           COMMENT '最近催收时间',
  `last_reminder_by`  VARCHAR(50)   DEFAULT NULL           COMMENT '最近催收人',
  `last_reminder_result` TINYINT    DEFAULT NULL           COMMENT '最近催收结果',
  `next_follow_up_date` DATE        DEFAULT NULL           COMMENT '下次跟进日期',
  `is_invoice`        TINYINT       NOT NULL DEFAULT 2      COMMENT '1开票 2不开票',
  `settle_status`     TINYINT       NOT NULL DEFAULT 1      COMMENT '1待收款 2部分收款 3全部结清 4已作废',
  `void_reason`       VARCHAR(255)  DEFAULT NULL            COMMENT '作废原因',
  `void_by`           VARCHAR(50)   DEFAULT NULL            COMMENT '作废操作人',
  `void_time`         DATETIME      DEFAULT NULL            COMMENT '作废时间',
  `snap_bill`         JSON          DEFAULT NULL            COMMENT '结算单快照JSON',
  `snap_bill_time`    DATETIME      DEFAULT NULL            COMMENT '结算单快照时间',
  `remark`            VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `is_deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`         VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`         VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`           INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`          VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`          VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`          DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`          DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_settle_no` (`settle_no`),
  UNIQUE KEY `uk_settle_request_id` (`request_id`),
  KEY `idx_customer_uuid` (`customer_uuid`),
  KEY `idx_settle_status` (`settle_status`),
  KEY `idx_settle_date` (`settle_date`),
  KEY `idx_settle_due_status` (`is_deleted`, `settle_status`, `due_date`, `uuid`),
  KEY `idx_settle_collection_queue` (`is_deleted`, `settle_status`, `last_reminder_time`, `due_date`, `uuid`),
  KEY `idx_report_settle_scope` (`is_deleted`, `settle_status`, `settle_date`, `customer_uuid`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `chk_settle_discount_nonnegative` CHECK (`discount_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算单主表';

-- -----------------------------------------------------------------------------
-- 3.3.10.1 biz_settle_collection_reminder 结算催收提醒流水
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_settle_collection_reminder`;
CREATE TABLE `biz_settle_collection_reminder` (
  `uuid` VARCHAR(36) NOT NULL COMMENT '催收记录主键',
  `settle_uuid` VARCHAR(36) NOT NULL COMMENT '关联结算单',
  `request_id` VARCHAR(64) NOT NULL COMMENT '客户端幂等请求号',
  `reminder_channel` TINYINT NOT NULL COMMENT '1电话 2微信 3短信 4上门 5其他',
  `reminder_result` TINYINT NOT NULL COMMENT '1已联系 2未接通 3承诺付款 4有异议 5其他',
  `contact_name` VARCHAR(100) DEFAULT NULL COMMENT '联系人',
  `reminder_time` DATETIME NOT NULL COMMENT '提醒时间',
  `next_follow_up_date` DATE DEFAULT NULL COMMENT '下次跟进日期',
  `operator_uuid` VARCHAR(36) NOT NULL COMMENT '操作人账号主键',
  `operator_name` VARCHAR(50) NOT NULL COMMENT '操作人姓名快照',
  `remark` VARCHAR(500) NOT NULL COMMENT '提醒结果说明',
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_settle_collection_request` (`settle_uuid`, `request_id`),
  KEY `idx_settle_collection_time` (`settle_uuid`, `reminder_time`, `uuid`),
  KEY `idx_settle_collection_follow_up` (`next_follow_up_date`, `settle_uuid`),
  CONSTRAINT `fk_settle_collection_order` FOREIGN KEY (`settle_uuid`) REFERENCES `biz_settle_order` (`uuid`) ON DELETE RESTRICT,
  CONSTRAINT `chk_settle_collection_channel` CHECK (`reminder_channel` BETWEEN 1 AND 5),
  CONSTRAINT `chk_settle_collection_result` CHECK (`reminder_result` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算催收提醒流水';

-- -----------------------------------------------------------------------------
-- 3.3.11 biz_settle_detail 结算关联加工单明细表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_settle_detail`;
CREATE TABLE `biz_settle_detail` (
  `uuid`          VARCHAR(36)   NOT NULL                COMMENT '明细主键',
  `settle_uuid`   VARCHAR(36)   NOT NULL                COMMENT '关联结算单',
  `order_uuid`    VARCHAR(36)   NOT NULL                COMMENT '关联加工单',
  `order_no`      VARCHAR(50)   DEFAULT NULL            COMMENT '冗余加工单号',
  `saw_amount`    DECIMAL(12,2) DEFAULT 0.00            COMMENT '本单锯纸费',
  `rewind_amount` DECIMAL(12,2) DEFAULT 0.00            COMMENT '本单复卷费',
  `service_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '本单服务工序费',
  `standard_process_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '优惠前标准加工费',
  `pricing_adjustment_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '最终加工费减标准加工费',
  `pricing_adjustment_reason` VARCHAR(255) DEFAULT NULL COMMENT '计价调整原因',
  `extra_amount`  DECIMAL(12,2) DEFAULT 0.00            COMMENT '本单附加费',
  `order_amount`  DECIMAL(12,2) NOT NULL DEFAULT 0.00  COMMENT '本单计入结算金额',
  `remark`        VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `is_deleted`    TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`     VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`     VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`       INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`      VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`      VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`      DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`      DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  KEY `idx_settle_uuid` (`settle_uuid`),
  KEY `idx_order_uuid` (`order_uuid`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算关联加工单明细表';

-- -----------------------------------------------------------------------------
-- 3.3.12 biz_receive_record 分次收款流水表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_receive_record`;
CREATE TABLE `biz_receive_record` (
  `uuid`           VARCHAR(36)   NOT NULL                COMMENT '收款流水主键',
  `settle_uuid`    VARCHAR(36)   NOT NULL                COMMENT '关联结算单',
  `request_id`     VARCHAR(64)   DEFAULT NULL            COMMENT '客户端幂等请求号',
  `request_hash`   CHAR(64)      DEFAULT NULL            COMMENT '收款请求载荷SHA-256',
  `receive_date`   DATETIME      NOT NULL                COMMENT '收款时间',
  `receive_amount` DECIMAL(12,2) NOT NULL                COMMENT '本次结清金额',
  `cash_amount`    DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '实际到账金额',
  `scrap_offset_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '废纸抵扣金额',
  `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠及尾差核销金额',
  `discount_reason` VARCHAR(255) DEFAULT NULL COMMENT '优惠及尾差核销原因',
  `discount_approval_uuid` VARCHAR(36) DEFAULT NULL COMMENT '超过阈值时关联审批记录',
  `discount_approved_by` VARCHAR(50) DEFAULT NULL COMMENT '优惠批准人或免审登记人',
  `scrap_weight`   DECIMAL(12,3) NOT NULL DEFAULT 0.000  COMMENT '废纸抵扣重量kg',
  `scrap_unit_price` DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '废纸抵扣折算单价',
  `receive_type`   TINYINT       NOT NULL DEFAULT 1      COMMENT '1普通收款 2废纸抵扣 3混合收款',
  `pay_method`     TINYINT       DEFAULT NULL            COMMENT '1现金 2转账 3微信 4支付宝；纯废纸抵扣可为空',
  `pay_no`         VARCHAR(100)  DEFAULT NULL            COMMENT '流水/凭证号',
  `operator`       VARCHAR(50)   DEFAULT NULL            COMMENT '收款登记人',
  `record_status`  TINYINT       NOT NULL DEFAULT 1      COMMENT '1有效 2已撤销',
  `cancel_time`    DATETIME      DEFAULT NULL            COMMENT '撤销时间',
  `cancel_by`      VARCHAR(50)   DEFAULT NULL            COMMENT '撤销人',
  `cancel_reason`  VARCHAR(255)  DEFAULT NULL            COMMENT '撤销原因',
  `remark`         VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `is_deleted`     TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`      VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`      VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`        INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`       VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`       VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`       DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`       DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_receive_settle_request` (`settle_uuid`, `request_id`),
  KEY `idx_settle_uuid` (`settle_uuid`),
  KEY `idx_receive_date` (`receive_date`),
  KEY `idx_receive_record_status` (`record_status`),
  KEY `idx_receive_record_type` (`receive_type`),
  KEY `idx_report_receive_scope` (`is_deleted`, `record_status`, `receive_date`, `settle_uuid`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `chk_receive_discount_nonnegative` CHECK (`discount_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分次收款流水表';

-- -----------------------------------------------------------------------------
-- 3.3.13 biz_settle_discount_approval 优惠审批
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_settle_discount_approval`;
CREATE TABLE `biz_settle_discount_approval` (
  `uuid`              VARCHAR(36)   NOT NULL,
  `settle_uuid`       VARCHAR(36)   NOT NULL,
  `request_id`        VARCHAR(64)   NOT NULL,
  `discount_amount`   DECIMAL(12,2) NOT NULL,
  `reason`            VARCHAR(255)  NOT NULL,
  `approval_status`   TINYINT       NOT NULL DEFAULT 1 COMMENT '1待审批 2已批准 3已使用 4已拒绝',
  `request_by`        VARCHAR(36)   NOT NULL,
  `request_by_name`   VARCHAR(50)   NOT NULL,
  `request_time`      DATETIME      NOT NULL,
  `approve_by`        VARCHAR(36)   DEFAULT NULL,
  `approve_by_name`   VARCHAR(50)   DEFAULT NULL,
  `approve_time`      DATETIME      DEFAULT NULL,
  `used_receive_uuid` VARCHAR(36)   DEFAULT NULL,
  `is_deleted`        TINYINT       NOT NULL DEFAULT 0,
  `create_by`         VARCHAR(50)   DEFAULT NULL,
  `update_by`         VARCHAR(50)   DEFAULT NULL,
  `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version`           INT           NOT NULL DEFAULT 1,
  `ext_str1`          VARCHAR(255)  DEFAULT NULL,
  `ext_str2`          VARCHAR(255)  DEFAULT NULL,
  `ext_num1`          DECIMAL(12,3) DEFAULT NULL,
  `ext_num2`          DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_discount_approval_request` (`settle_uuid`, `request_id`),
  UNIQUE KEY `uk_discount_approval_receive` (`used_receive_uuid`),
  KEY `idx_discount_approval_settle_status` (`settle_uuid`, `approval_status`),
  CONSTRAINT `chk_discount_approval_amount_positive` CHECK (`discount_amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算优惠及尾差审批记录';

-- =============================================================================
-- 五、系统辅助
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 3.3.13 sys_operation_log 操作日志表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `uuid`         VARCHAR(36)   NOT NULL                COMMENT '日志主键',
  `biz_type`     VARCHAR(50)   NOT NULL                COMMENT '业务类型（加工单/出库单/结算单等）',
  `biz_uuid`     VARCHAR(36)   NOT NULL                COMMENT '关联业务主键',
  `biz_no`       VARCHAR(50)   DEFAULT NULL            COMMENT '冗余业务单号',
  `action_type`  VARCHAR(50)   NOT NULL                COMMENT '动作类型（字段修改/打印/回录/结算/收款/作废卷号/删除/超差放行）',
  `field_name`   VARCHAR(100)  DEFAULT NULL            COMMENT '字段级日志：变更字段名',
  `old_value`    TEXT          DEFAULT NULL            COMMENT '修改前值',
  `new_value`    TEXT          DEFAULT NULL            COMMENT '修改后值',
  `operator`     VARCHAR(50)   NOT NULL                COMMENT '操作人',
  `operate_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `remark`       TEXT          DEFAULT NULL            COMMENT '备注（如补打原因、超差放行原因）',
  `is_deleted`   TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`    VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`    VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`      INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`     VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`     VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`     DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`     DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  KEY `idx_biz` (`biz_type`, `biz_uuid`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_operate_time` (`operate_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- -----------------------------------------------------------------------------
-- 3.3.14 sys_user 系统用户表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `uuid`            VARCHAR(36)  NOT NULL                COMMENT '用户主键',
  `username`        VARCHAR(50)  NOT NULL                COMMENT '登录名',
  `password_hash`   VARCHAR(100) NOT NULL                COMMENT 'BCrypt密码哈希',
  `real_name`       VARCHAR(50)  NOT NULL                COMMENT '姓名',
  `role_code`       VARCHAR(30)  NOT NULL                COMMENT '角色编码 admin/operator/finance/warehouse',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '1启用 2停用',
  `last_login_time` DATETIME     DEFAULT NULL            COMMENT '最后登录时间',
  `remark`          VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`       VARCHAR(50)  DEFAULT NULL            COMMENT '创建人',
  `update_by`       VARCHAR(50)  DEFAULT NULL            COMMENT '更新人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`        VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`        VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`        DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值1',
  `ext_num2`        DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_role` (`role_code`),
  KEY `idx_sys_user_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- -----------------------------------------------------------------------------
-- 3.3.15 sys_user_session 系统用户会话表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_session`;
CREATE TABLE `sys_user_session` (
  `uuid`         VARCHAR(36)  NOT NULL                COMMENT '会话主键',
  `token`        VARCHAR(64)  NOT NULL                COMMENT '访问令牌',
  `user_uuid`    VARCHAR(36)  NOT NULL                COMMENT '用户主键',
  `expire_time`  DATETIME     NOT NULL                COMMENT '过期时间',
  `revoked_time` DATETIME     DEFAULT NULL            COMMENT '退出/作废时间',
  `is_deleted`   TINYINT      NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`    VARCHAR(50)  DEFAULT NULL            COMMENT '创建人',
  `update_by`    VARCHAR(50)  DEFAULT NULL            COMMENT '更新人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`      INT          NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`     VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`     VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`     DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值1',
  `ext_num2`     DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_sys_user_session_token` (`token`),
  KEY `idx_sys_user_session_user` (`user_uuid`),
  KEY `idx_sys_user_session_expire` (`expire_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户会话表';

-- -----------------------------------------------------------------------------
-- 3.3.16 sys_no_rule 系统单号规则表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_no_rule`;
CREATE TABLE `sys_no_rule` (
  `uuid`          VARCHAR(36)  NOT NULL                COMMENT '主键UUID',
  `biz_type`      VARCHAR(50)  NOT NULL                COMMENT '业务类型',
  `rule_name`     VARCHAR(100) NOT NULL                COMMENT '规则名称',
  `prefix`        VARCHAR(20)  NOT NULL                COMMENT '单号前缀',
  `pattern_type`  TINYINT      NOT NULL DEFAULT 1      COMMENT '格式 1前缀+日期+序号 2前缀+序号',
  `date_pattern`  VARCHAR(20)  DEFAULT 'yyyyMMdd'      COMMENT '日期格式 yyyyMMdd/yyyyMM/yyyy',
  `serial_length` INT          NOT NULL DEFAULT 4      COMMENT '流水位数',
  `reset_cycle`   TINYINT      NOT NULL DEFAULT 1      COMMENT '重置周期 0不重置 1按日 2按月 3按年',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 1启用 0停用',
  `remark`        VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`     VARCHAR(50)  DEFAULT NULL            COMMENT '创建人',
  `update_by`     VARCHAR(50)  DEFAULT NULL            COMMENT '更新人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`       INT          NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`      VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`      VARCHAR(255) DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`      DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值1',
  `ext_num2`      DECIMAL(12,3) DEFAULT NULL           COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_sys_no_rule_biz` (`biz_type`, `is_deleted`),
  KEY `idx_sys_no_rule_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统单号规则表';

INSERT INTO `sys_no_rule`
(`uuid`, `biz_type`, `rule_name`, `prefix`, `pattern_type`, `date_pattern`, `serial_length`, `reset_cycle`, `status`, `remark`)
VALUES
('no-rule-process-order', 'process_order', '加工单号', 'JG', 1, 'yyyyMMdd', 4, 1, 1, '默认加工单号：JG+日期+4位日流水'),
('no-rule-delivery-order', 'delivery_order', '出库单号', 'CK', 1, 'yyyyMMdd', 4, 1, 1, '默认出库单号：CK+日期+4位日流水'),
('no-rule-settle-order', 'settle_order', '结算单号', 'JS', 1, 'yyyyMMdd', 4, 1, 1, '默认结算单号：JS+日期+4位日流水'),
('no-rule-finish-roll', 'finish_roll', '成品卷号', 'A', 2, 'yyyyMMdd', 6, 0, 1, '默认成品卷号：前缀+6位全局流水'),
('no-rule-customer', 'customer', '客户编码', 'KH', 2, 'yyyyMMdd', 6, 0, 1, '默认客户编码：KH+6位全局流水'),
('no-rule-paper', 'paper', '纸张编码', 'ZZ', 2, 'yyyyMMdd', 6, 0, 1, '默认纸张编码：ZZ+6位全局流水'),
('no-rule-machine', 'machine', '机台编码', 'JT', 2, 'yyyyMMdd', 6, 0, 1, '默认机台编码：JT+6位全局流水'),
('no-rule-warehouse', 'warehouse', '仓库编码', 'CKD', 2, 'yyyyMMdd', 6, 0, 1, '默认仓库编码：CKD+6位全局流水');

-- -----------------------------------------------------------------------------
-- 当前版本补充表：工艺草稿、卷号序列、系统配置、备份任务和站内通知
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_process_config_draft`;
CREATE TABLE `biz_process_config_draft` (
  `uuid` VARCHAR(36) NOT NULL,
  `order_uuid` VARCHAR(36) NOT NULL,
  `original_uuid` VARCHAR(36) NOT NULL,
  `process_mode` TINYINT NOT NULL,
  `main_step_type` TINYINT DEFAULT NULL,
  `config_json` JSON NOT NULL,
  `preview_json` JSON DEFAULT NULL,
  `config_status` TINYINT NOT NULL DEFAULT 0,
  `last_error` VARCHAR(500) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(64) DEFAULT NULL,
  `update_by` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(18,6) DEFAULT NULL,
  `ext_num2` DECIMAL(18,6) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_config_draft_roll` (`order_uuid`, `original_uuid`, `is_deleted`),
  KEY `idx_config_draft_order_status` (`order_uuid`, `config_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加工单单卷工艺配置草稿';

DROP TABLE IF EXISTS `sys_roll_no_sequence`;
CREATE TABLE `sys_roll_no_sequence` (
  `sequence_key` VARCHAR(50) NOT NULL,
  `current_value` BIGINT NOT NULL DEFAULT 0,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sequence_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局卷号序列表';

DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item` (
  `uuid` VARCHAR(36) NOT NULL,
  `dict_type` VARCHAR(50) NOT NULL,
  `dict_name` VARCHAR(80) NOT NULL,
  `item_code` VARCHAR(50) NOT NULL,
  `item_name` VARCHAR(80) NOT NULL,
  `item_value` INT DEFAULT NULL,
  `sort_no` INT NOT NULL DEFAULT 100,
  `status` TINYINT NOT NULL DEFAULT 1,
  `built_in` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_sys_dict_item_code` (`dict_type`, `item_code`, `is_deleted`),
  KEY `idx_sys_dict_item_type` (`dict_type`),
  KEY `idx_sys_dict_item_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统数据字典项';

DROP TABLE IF EXISTS `sys_config_item`;
CREATE TABLE `sys_config_item` (
  `uuid` VARCHAR(36) NOT NULL,
  `config_group` VARCHAR(50) NOT NULL,
  `config_key` VARCHAR(80) NOT NULL,
  `config_name` VARCHAR(80) NOT NULL,
  `config_value` VARCHAR(255) NOT NULL,
  `value_type` VARCHAR(20) NOT NULL DEFAULT 'string',
  `unit` VARCHAR(20) DEFAULT NULL,
  `sort_no` INT NOT NULL DEFAULT 100,
  `status` TINYINT NOT NULL DEFAULT 1,
  `built_in` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_sys_config_key` (`config_key`, `is_deleted`),
  KEY `idx_sys_config_group` (`config_group`),
  KEY `idx_sys_config_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数配置';

DROP TABLE IF EXISTS `sys_backup_task`;
CREATE TABLE `sys_backup_task` (
  `uuid` VARCHAR(36) NOT NULL,
  `task_type` VARCHAR(20) NOT NULL,
  `backup_id` VARCHAR(15) DEFAULT NULL,
  `task_status` VARCHAR(20) NOT NULL,
  `started_at` DATETIME NOT NULL,
  `finished_at` DATETIME DEFAULT NULL,
  `duration_ms` BIGINT DEFAULT NULL,
  `operator` VARCHAR(50) NOT NULL,
  `message` VARCHAR(255) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  KEY `idx_backup_task_started` (`started_at`),
  KEY `idx_backup_task_status` (`task_status`, `started_at`),
  KEY `idx_backup_task_backup` (`backup_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备份与恢复演练任务记录';

DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification` (
  `uuid` VARCHAR(36) NOT NULL,
  `recipient_uuid` VARCHAR(36) NOT NULL,
  `notification_type` VARCHAR(30) NOT NULL,
  `severity` VARCHAR(10) NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `source_type` VARCHAR(30) NOT NULL,
  `source_uuid` VARCHAR(36) NOT NULL,
  `read_at` DATETIME DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_notification_source` (`recipient_uuid`, `notification_type`, `source_uuid`),
  KEY `idx_notification_recipient_time` (`recipient_uuid`, `create_time`),
  KEY `idx_notification_recipient_read` (`recipient_uuid`, `read_at`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统站内通知';

DROP TABLE IF EXISTS `sys_operational_alert_state`;
CREATE TABLE `sys_operational_alert_state` (
  `alert_key` VARCHAR(64) NOT NULL,
  `state_code` VARCHAR(30) NOT NULL,
  `transition_no` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`alert_key`),
  CONSTRAINT `chk_operational_alert_transition_no` CHECK (`transition_no` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨实例运行态告警状态';

DROP TABLE IF EXISTS `sys_export_task`;
CREATE TABLE `sys_export_task` (
  `uuid` VARCHAR(36) NOT NULL, `request_id` VARCHAR(64) NOT NULL,
  `task_type` VARCHAR(30) NOT NULL, `module_code` VARCHAR(30) NOT NULL,
  `operation_code` VARCHAR(50) NOT NULL, `task_name` VARCHAR(120) NOT NULL,
  `source_uuid` VARCHAR(36) NOT NULL, `source_path` VARCHAR(160) DEFAULT NULL,
  `request_payload` TEXT DEFAULT NULL, `query_snapshot_uuid` VARCHAR(36) DEFAULT NULL,
  `metric_release_uuid` VARCHAR(36) DEFAULT NULL,
  `requester_uuid` VARCHAR(36) NOT NULL,
  `requester_name` VARCHAR(50) NOT NULL, `task_status` TINYINT NOT NULL DEFAULT 1,
  `progress` TINYINT NOT NULL DEFAULT 0, `file_name` VARCHAR(255) DEFAULT NULL,
  `file_path` VARCHAR(500) DEFAULT NULL, `content_type` VARCHAR(120) DEFAULT NULL,
  `file_size` BIGINT DEFAULT NULL,
  `error_message` VARCHAR(500) DEFAULT NULL, `started_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL, `acknowledged_at` DATETIME DEFAULT NULL,
  `expires_at` DATETIME NOT NULL, `attempt_count` INT NOT NULL DEFAULT 0,
  `max_attempts` INT NOT NULL DEFAULT 3, `heartbeat_at` DATETIME DEFAULT NULL,
  `worker_id` VARCHAR(100) DEFAULT NULL, `downloaded_at` DATETIME DEFAULT NULL,
  `download_count` INT NOT NULL DEFAULT 0, `cancelled_at` DATETIME DEFAULT NULL,
  `cancelled_by` VARCHAR(36) DEFAULT NULL, `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL, `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1, `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL, `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_export_task_request` (`requester_uuid`, `request_id`),
  KEY `idx_export_task_owner_time` (`requester_uuid`, `create_time`, `uuid`),
  KEY `idx_export_task_owner_module_operation_time` (`requester_uuid`, `module_code`, `operation_code`, `create_time`, `uuid`),
  KEY `idx_export_task_query_snapshot` (`query_snapshot_uuid`),
  KEY `idx_export_task_metric_release_time` (`metric_release_uuid`, `create_time`, `uuid`),
  KEY `idx_export_task_owner_status` (`requester_uuid`, `task_status`, `acknowledged_at`),
  KEY `idx_export_task_expiry` (`expires_at`, `task_status`),
  KEY `idx_export_task_dispatch` (`task_status`, `heartbeat_at`, `create_time`),
  KEY `idx_export_task_status_completed` (`task_status`, `completed_at`),
  CONSTRAINT `chk_export_task_status` CHECK (`task_status` BETWEEN 1 AND 6),
  CONSTRAINT `chk_export_task_progress` CHECK (`progress` BETWEEN 0 AND 100),
  CONSTRAINT `chk_export_task_attempts` CHECK (`attempt_count` >= 0 AND `max_attempts` BETWEEN 1 AND 10),
  CONSTRAINT `chk_export_task_download_count` CHECK (`download_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步导出任务中心';

DROP TABLE IF EXISTS `rpt_report_saved_view`;
DROP TABLE IF EXISTS `rpt_report_snapshot_reference`;
DROP TABLE IF EXISTS `rpt_report_snapshot`;
DROP TABLE IF EXISTS `rpt_metric_value`;
DROP TABLE IF EXISTS `rpt_metric_value_stage`;
DROP TABLE IF EXISTS `rpt_metric_materialization_state`;
DROP TABLE IF EXISTS `rpt_metric_materialization_segment`;
DROP TABLE IF EXISTS `rpt_metric_materialization_job`;
DROP TABLE IF EXISTS `rpt_metric_release_item`;
DROP TABLE IF EXISTS `rpt_metric_release`;
DROP TABLE IF EXISTS `rpt_metric_version`;
DROP TABLE IF EXISTS `rpt_metric_definition`;

CREATE TABLE `rpt_metric_definition` (
  `uuid` VARCHAR(36) NOT NULL,
  `metric_code` VARCHAR(64) NOT NULL,
  `metric_name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) NOT NULL DEFAULT '',
  `value_type` VARCHAR(20) NOT NULL,
  `unit_code` VARCHAR(20) NOT NULL,
  `display_scale` TINYINT UNSIGNED NOT NULL DEFAULT 2,
  `display_order` INT UNSIGNED NOT NULL DEFAULT 0,
  `is_enabled` TINYINT NOT NULL DEFAULT 1,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_definition_code` (`metric_code`),
  KEY `idx_metric_definition_enabled_order` (`is_deleted`, `is_enabled`, `display_order`, `metric_code`),
  CONSTRAINT `chk_metric_definition_value_type` CHECK (`value_type` IN ('INTEGER', 'DECIMAL', 'MONEY', 'PERCENT')),
  CONSTRAINT `chk_metric_definition_enabled` CHECK (`is_enabled` IN (0, 1)),
  CONSTRAINT `chk_metric_definition_scale` CHECK (`display_scale` <= 6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标稳定标识';

CREATE TABLE `rpt_metric_version` (
  `uuid` VARCHAR(36) NOT NULL,
  `metric_uuid` VARCHAR(36) NOT NULL,
  `version_no` INT UNSIGNED NOT NULL,
  `implementation_key` VARCHAR(100) NOT NULL,
  `definition_json` JSON NOT NULL,
  `definition_checksum` CHAR(64) NOT NULL,
  `version_status` TINYINT NOT NULL DEFAULT 1,
  `locked_at` DATETIME DEFAULT NULL,
  `locked_by` VARCHAR(36) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_version_number` (`metric_uuid`, `version_no`),
  UNIQUE KEY `uk_metric_version_identity` (`uuid`, `metric_uuid`),
  KEY `idx_metric_version_status` (`metric_uuid`, `version_status`, `is_deleted`),
  CONSTRAINT `fk_metric_version_definition` FOREIGN KEY (`metric_uuid`)
    REFERENCES `rpt_metric_definition` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_metric_version_number` CHECK (`version_no` >= 1),
  CONSTRAINT `chk_metric_version_status` CHECK (`version_status` IN (1, 2)),
  CONSTRAINT `chk_metric_version_lock` CHECK (
    (`version_status` = 1 AND `locked_at` IS NULL) OR
    (`version_status` = 2 AND `locked_at` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标不可变版本';

CREATE TABLE `rpt_metric_release` (
  `uuid` VARCHAR(36) NOT NULL,
  `release_code` VARCHAR(40) NOT NULL,
  `release_name` VARCHAR(120) NOT NULL,
  `release_status` TINYINT NOT NULL DEFAULT 1,
  `release_checksum` CHAR(64) DEFAULT NULL,
  `published_at` DATETIME DEFAULT NULL,
  `published_by` VARCHAR(36) DEFAULT NULL,
  `retired_at` DATETIME DEFAULT NULL,
  `retired_by` VARCHAR(36) DEFAULT NULL,
  `active_slot` TINYINT GENERATED ALWAYS AS (
    CASE WHEN `release_status` = 2 AND `is_deleted` = 0 THEN 1 ELSE NULL END
  ) STORED,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_release_code` (`release_code`),
  UNIQUE KEY `uk_metric_release_active` (`active_slot`),
  KEY `idx_metric_release_status_time` (`is_deleted`, `release_status`, `published_at`),
  CONSTRAINT `chk_metric_release_status` CHECK (`release_status` IN (1, 2, 3)),
  CONSTRAINT `chk_metric_release_lifecycle` CHECK (
    (`release_status` = 1 AND `published_at` IS NULL AND `retired_at` IS NULL) OR
    (`release_status` = 2 AND `published_at` IS NOT NULL AND `retired_at` IS NULL) OR
    (`release_status` = 3 AND `published_at` IS NOT NULL AND `retired_at` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标原子发布包';

CREATE TABLE `rpt_metric_release_item` (
  `uuid` VARCHAR(36) NOT NULL,
  `release_uuid` VARCHAR(36) NOT NULL,
  `metric_uuid` VARCHAR(36) NOT NULL,
  `metric_version_uuid` VARCHAR(36) NOT NULL,
  `display_order` INT UNSIGNED NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_release_item_metric` (`release_uuid`, `metric_uuid`),
  KEY `idx_metric_release_item_version` (`metric_version_uuid`, `metric_uuid`),
  CONSTRAINT `fk_metric_release_item_release` FOREIGN KEY (`release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_release_item_definition` FOREIGN KEY (`metric_uuid`)
    REFERENCES `rpt_metric_definition` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_release_item_version` FOREIGN KEY (`metric_version_uuid`, `metric_uuid`)
    REFERENCES `rpt_metric_version` (`uuid`, `metric_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布包内逐指标版本绑定';

DROP TABLE IF EXISTS `rpt_report_subscription_run`;
DROP TABLE IF EXISTS `rpt_report_subscription_recipient`;
DROP TABLE IF EXISTS `rpt_report_subscription`;
DROP TABLE IF EXISTS `rpt_alert_event`;
DROP TABLE IF EXISTS `rpt_alert_rule`;
DROP TABLE IF EXISTS `rpt_alert_signal_definition`;

CREATE TABLE `rpt_report_subscription` (
  `uuid` VARCHAR(36) NOT NULL,
  `owner_uuid` VARCHAR(36) NOT NULL,
  `subscription_name` VARCHAR(100) NOT NULL,
  `report_path` VARCHAR(160) NOT NULL DEFAULT '/reports/overview',
  `schedule_type` TINYINT NOT NULL,
  `execution_time` TIME NOT NULL,
  `week_day` TINYINT DEFAULT NULL,
  `month_day` TINYINT DEFAULT NULL,
  `timezone` VARCHAR(40) NOT NULL DEFAULT 'Asia/Shanghai',
  `report_query` JSON NOT NULL,
  `period_policy` TINYINT NOT NULL DEFAULT 1,
  `release_policy` TINYINT NOT NULL DEFAULT 1,
  `pinned_release_uuid` VARCHAR(36) DEFAULT NULL,
  `delivery_channel` VARCHAR(20) NOT NULL DEFAULT 'DOWNLOAD_CENTER',
  `is_enabled` TINYINT NOT NULL DEFAULT 1,
  `next_run_at` DATETIME NOT NULL,
  `last_scheduled_at` DATETIME DEFAULT NULL,
  `last_error_message` VARCHAR(500) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `active_name` VARCHAR(100) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `subscription_name` ELSE NULL END
  ) STORED,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_subscription_owner_active_name` (`owner_uuid`, `active_name`),
  KEY `idx_report_subscription_due` (`is_deleted`, `is_enabled`, `next_run_at`, `uuid`),
  KEY `idx_report_subscription_release` (`pinned_release_uuid`),
  CONSTRAINT `fk_report_subscription_owner` FOREIGN KEY (`owner_uuid`)
    REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_report_subscription_release` FOREIGN KEY (`pinned_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_subscription_schedule_type` CHECK (`schedule_type` IN (1, 2, 3)),
  CONSTRAINT `chk_report_subscription_period_policy` CHECK (`period_policy` IN (1, 2, 3, 4)),
  CONSTRAINT `chk_report_subscription_schedule_fields` CHECK (
    (`schedule_type` = 1 AND `week_day` IS NULL AND `month_day` IS NULL) OR
    (`schedule_type` = 2 AND `week_day` BETWEEN 1 AND 7 AND `month_day` IS NULL) OR
    (`schedule_type` = 3 AND `week_day` IS NULL AND `month_day` BETWEEN 1 AND 28)
  ),
  CONSTRAINT `chk_report_subscription_release_policy` CHECK (
    (`release_policy` = 1 AND `pinned_release_uuid` IS NULL) OR
    (`release_policy` = 2 AND `pinned_release_uuid` IS NOT NULL)
  ),
  CONSTRAINT `chk_report_subscription_channel` CHECK (`delivery_channel` = 'DOWNLOAD_CENTER'),
  CONSTRAINT `chk_report_subscription_enabled` CHECK (`is_enabled` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表定时订阅';

CREATE TABLE `rpt_report_subscription_recipient` (
  `uuid` VARCHAR(36) NOT NULL,
  `subscription_uuid` VARCHAR(36) NOT NULL,
  `recipient_uuid` VARCHAR(36) NOT NULL,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_subscription_recipient` (`subscription_uuid`, `recipient_uuid`),
  KEY `idx_report_subscription_recipient_user` (`recipient_uuid`, `subscription_uuid`),
  CONSTRAINT `fk_report_subscription_recipient_subscription` FOREIGN KEY (`subscription_uuid`)
    REFERENCES `rpt_report_subscription` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_report_subscription_recipient_user` FOREIGN KEY (`recipient_uuid`)
    REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表订阅接收人';

CREATE TABLE `rpt_report_subscription_run` (
  `uuid` VARCHAR(36) NOT NULL,
  `subscription_uuid` VARCHAR(36) NOT NULL,
  `scheduled_for` DATETIME NOT NULL,
  `metric_release_uuid` VARCHAR(36) DEFAULT NULL,
  `run_status` TINYINT NOT NULL DEFAULT 1,
  `planned_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `dispatched_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `failed_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `error_message` VARCHAR(500) DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_subscription_run_slot` (`subscription_uuid`, `scheduled_for`),
  KEY `idx_report_subscription_run_status` (`run_status`, `scheduled_for`, `uuid`),
  KEY `idx_report_subscription_run_release` (`metric_release_uuid`, `scheduled_for`),
  CONSTRAINT `fk_report_subscription_run_subscription` FOREIGN KEY (`subscription_uuid`)
    REFERENCES `rpt_report_subscription` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_report_subscription_run_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_subscription_run_status` CHECK (`run_status` IN (1, 2, 3, 4)),
  CONSTRAINT `chk_report_subscription_run_counts` CHECK (
    `dispatched_count` + `failed_count` <= `planned_count`
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表订阅调度运行记录';

CREATE TABLE `rpt_alert_signal_definition` (
  `signal_code` VARCHAR(64) NOT NULL,
  `signal_name` VARCHAR(100) NOT NULL,
  `unit_code` VARCHAR(20) NOT NULL,
  `description` VARCHAR(500) NOT NULL DEFAULT '',
  `is_enabled` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`signal_code`),
  CONSTRAINT `chk_alert_signal_enabled` CHECK (`is_enabled` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表派生告警信号定义';

CREATE TABLE `rpt_alert_rule` (
  `uuid` VARCHAR(36) NOT NULL,
  `signal_code` VARCHAR(64) NOT NULL,
  `rule_name` VARCHAR(120) NOT NULL,
  `scope_type` TINYINT NOT NULL,
  `customer_uuid` VARCHAR(36) DEFAULT NULL,
  `paper_uuid` VARCHAR(36) DEFAULT NULL,
  `process_type` TINYINT DEFAULT NULL,
  `comparison_operator` VARCHAR(10) NOT NULL DEFAULT 'GTE',
  `threshold_value` DECIMAL(18,6) NOT NULL,
  `severity` TINYINT NOT NULL DEFAULT 1,
  `is_enabled` TINYINT NOT NULL DEFAULT 1,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `active_rule_key` VARCHAR(180) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN CONCAT(`signal_code`, ':', `scope_type`, ':',
      COALESCE(`customer_uuid`, ''), ':', COALESCE(`paper_uuid`, ''), ':', COALESCE(`process_type`, ''))
    ELSE NULL END
  ) STORED,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_alert_rule_active_scope` (`active_rule_key`),
  KEY `idx_alert_rule_resolution` (`signal_code`, `is_deleted`, `is_enabled`, `scope_type`),
  KEY `idx_alert_rule_customer` (`customer_uuid`, `signal_code`),
  KEY `idx_alert_rule_paper` (`paper_uuid`, `signal_code`),
  CONSTRAINT `fk_alert_rule_signal` FOREIGN KEY (`signal_code`)
    REFERENCES `rpt_alert_signal_definition` (`signal_code`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_rule_customer` FOREIGN KEY (`customer_uuid`)
    REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_rule_paper` FOREIGN KEY (`paper_uuid`)
    REFERENCES `sys_paper` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_alert_rule_scope_type` CHECK (`scope_type` IN (1, 2, 3, 4)),
  CONSTRAINT `chk_alert_rule_scope_fields` CHECK (
    (`scope_type` = 1 AND `customer_uuid` IS NULL AND `paper_uuid` IS NULL AND `process_type` IS NULL) OR
    (`scope_type` = 2 AND `customer_uuid` IS NOT NULL AND `paper_uuid` IS NULL AND `process_type` IS NULL) OR
    (`scope_type` = 3 AND `customer_uuid` IS NULL AND `paper_uuid` IS NOT NULL AND `process_type` IS NULL) OR
    (`scope_type` = 4 AND `customer_uuid` IS NULL AND `paper_uuid` IS NULL AND `process_type` IN (1, 2))
  ),
  CONSTRAINT `chk_alert_rule_operator` CHECK (`comparison_operator` IN ('GT', 'GTE', 'LT', 'LTE')),
  CONSTRAINT `chk_alert_rule_threshold` CHECK (`threshold_value` >= 0),
  CONSTRAINT `chk_alert_rule_severity` CHECK (`severity` IN (1, 2)),
  CONSTRAINT `chk_alert_rule_enabled` CHECK (`is_enabled` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表分层告警阈值规则';

CREATE TABLE `rpt_alert_event` (
  `uuid` VARCHAR(36) NOT NULL,
  `rule_uuid` VARCHAR(36) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `event_key` CHAR(64) NOT NULL,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `dimension_hash` CHAR(64) NOT NULL,
  `metric_value` DECIMAL(20,6) NOT NULL,
  `threshold_value` DECIMAL(18,6) NOT NULL,
  `severity` TINYINT NOT NULL,
  `event_status` TINYINT NOT NULL DEFAULT 1,
  `occurrence_count` INT UNSIGNED NOT NULL DEFAULT 1,
  `first_detected_at` DATETIME NOT NULL,
  `last_detected_at` DATETIME NOT NULL,
  `resolved_at` DATETIME DEFAULT NULL,
  `acknowledged_at` DATETIME DEFAULT NULL,
  `acknowledged_by` VARCHAR(36) DEFAULT NULL,
  `ignored_at` DATETIME DEFAULT NULL,
  `ignored_by` VARCHAR(36) DEFAULT NULL,
  `ignore_reason` VARCHAR(500) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_alert_event_key` (`event_key`),
  KEY `idx_alert_event_status_time` (`is_deleted`, `event_status`, `last_detected_at`, `uuid`),
  KEY `idx_alert_event_rule_period` (`rule_uuid`, `period_start`, `period_end`),
  KEY `idx_alert_event_release` (`metric_release_uuid`, `period_end`),
  KEY `idx_alert_event_dimension` (`dimension_hash`, `period_end`),
  KEY `idx_alert_event_acknowledged` (`event_status`, `acknowledged_at`),
  KEY `idx_alert_event_ack_by` (`acknowledged_by`),
  KEY `idx_alert_event_ignore_by` (`ignored_by`),
  CONSTRAINT `fk_alert_event_rule` FOREIGN KEY (`rule_uuid`)
    REFERENCES `rpt_alert_rule` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_event_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_event_ack_by` FOREIGN KEY (`acknowledged_by`)
    REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_event_ignore_by` FOREIGN KEY (`ignored_by`)
    REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_alert_event_period` CHECK (`period_end` >= `period_start`),
  CONSTRAINT `chk_alert_event_severity` CHECK (`severity` IN (1, 2)),
  CONSTRAINT `chk_alert_event_status` CHECK (`event_status` IN (1, 2, 3)),
  CONSTRAINT `chk_alert_event_occurrences` CHECK (`occurrence_count` >= 1),
  CONSTRAINT `chk_alert_event_resolution` CHECK (
    (`event_status` = 2 AND `resolved_at` IS NOT NULL) OR (`event_status` <> 2 AND `resolved_at` IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表告警事件审计与去重';

INSERT INTO `rpt_alert_signal_definition`
  (`signal_code`, `signal_name`, `unit_code`, `description`)
VALUES
  ('LOSS_RATIO', '损耗率', 'PERCENT', '原纸投入与成品产出的损耗比例'),
  ('UNRECEIVED_RATIO', '已结算未收占比', 'PERCENT', '已结算未收金额占已结算应收比例');

INSERT INTO `rpt_alert_rule`
  (`uuid`, `signal_code`, `rule_name`, `scope_type`, `comparison_operator`, `threshold_value`, `severity`)
VALUES
  ('rpt-alert-loss-global-v1', 'LOSS_RATIO', '全局损耗率预警', 1, 'GTE', 5.000000, 2),
  ('rpt-alert-unreceived-global-v1', 'UNRECEIVED_RATIO', '全局未收占比预警', 1, 'GTE', 35.000000, 1);

CREATE TABLE `rpt_metric_materialization_job` (
  `uuid` VARCHAR(36) NOT NULL,
  `task_id` VARCHAR(64) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `job_status` TINYINT NOT NULL DEFAULT 1,
  `retry_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `lease_owner` VARCHAR(100) DEFAULT NULL,
  `lease_until` DATETIME DEFAULT NULL,
  `fencing_token` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `requested_by` VARCHAR(36) DEFAULT NULL,
  `started_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `error_message` VARCHAR(1000) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_materialization_task` (`task_id`),
  KEY `idx_metric_materialization_claim` (`job_status`, `lease_until`, `create_time`, `uuid`),
  KEY `idx_metric_materialization_release_period` (`metric_release_uuid`, `period_start`, `period_end`),
  CONSTRAINT `fk_metric_materialization_job_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_materialization_job_user` FOREIGN KEY (`requested_by`)
    REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_metric_materialization_job_period` CHECK (`period_end` >= `period_start`),
  CONSTRAINT `chk_metric_materialization_job_status` CHECK (`job_status` IN (1, 2, 3, 4, 5)),
  CONSTRAINT `chk_metric_materialization_job_lease` CHECK (
    (`job_status` = 2 AND `lease_owner` IS NOT NULL AND `lease_until` IS NOT NULL) OR (`job_status` <> 2)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标物化任务';

CREATE TABLE `rpt_metric_materialization_segment` (
  `uuid` VARCHAR(36) NOT NULL,
  `job_uuid` VARCHAR(36) NOT NULL,
  `segment_key` VARCHAR(100) NOT NULL,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `segment_status` TINYINT NOT NULL DEFAULT 1,
  `retry_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `lease_owner` VARCHAR(100) DEFAULT NULL,
  `lease_until` DATETIME DEFAULT NULL,
  `fencing_token` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `row_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `result_checksum` CHAR(64) DEFAULT NULL,
  `started_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `error_message` VARCHAR(1000) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_materialization_segment` (`job_uuid`, `segment_key`),
  KEY `idx_metric_materialization_segment_claim` (`segment_status`, `lease_until`, `create_time`, `uuid`),
  KEY `idx_metric_materialization_segment_period` (`period_start`, `period_end`, `segment_status`),
  CONSTRAINT `fk_metric_materialization_segment_job` FOREIGN KEY (`job_uuid`)
    REFERENCES `rpt_metric_materialization_job` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_metric_materialization_segment_period` CHECK (`period_end` >= `period_start`),
  CONSTRAINT `chk_metric_materialization_segment_status` CHECK (`segment_status` IN (1, 2, 3, 4, 5)),
  CONSTRAINT `chk_metric_materialization_segment_lease` CHECK (
    (`segment_status` = 2 AND `lease_owner` IS NOT NULL AND `lease_until` IS NOT NULL) OR (`segment_status` <> 2)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标物化分片';

CREATE TABLE `rpt_metric_materialization_state` (
  `uuid` VARCHAR(36) NOT NULL,
  `task_id` VARCHAR(64) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `metric_uuid` VARCHAR(36) NOT NULL,
  `metric_version_uuid` VARCHAR(36) NOT NULL,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `dimension_set_code` VARCHAR(64) NOT NULL,
  `materialization_status` TINYINT NOT NULL DEFAULT 1,
  `retry_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `active_generation_uuid` VARCHAR(36) DEFAULT NULL,
  `source_as_of` DATETIME DEFAULT NULL,
  `materialized_at` DATETIME DEFAULT NULL,
  `result_checksum` CHAR(64) DEFAULT NULL,
  `error_message` VARCHAR(1000) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_materialization_coverage`
    (`metric_release_uuid`, `metric_version_uuid`, `period_start`, `period_end`, `dimension_set_code`),
  KEY `idx_metric_materialization_state_task` (`task_id`, `materialization_status`, `uuid`),
  KEY `idx_metric_materialization_state_period` (`period_start`, `period_end`, `materialization_status`),
  KEY `idx_metric_materialization_state_generation` (`active_generation_uuid`),
  CONSTRAINT `fk_metric_materialization_state_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_materialization_state_metric` FOREIGN KEY (`metric_uuid`)
    REFERENCES `rpt_metric_definition` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_materialization_state_version` FOREIGN KEY (`metric_version_uuid`, `metric_uuid`)
    REFERENCES `rpt_metric_version` (`uuid`, `metric_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_metric_materialization_state_period` CHECK (`period_end` >= `period_start`),
  CONSTRAINT `chk_metric_materialization_state_status` CHECK (`materialization_status` IN (1, 2, 3)),
  CONSTRAINT `chk_metric_materialization_state_publication` CHECK (
    (`materialization_status` = 2 AND `active_generation_uuid` IS NOT NULL AND `materialized_at` IS NOT NULL) OR
    (`materialization_status` <> 2)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐指标版本物化覆盖状态';

CREATE TABLE `rpt_metric_value_stage` (
  `uuid` VARCHAR(36) NOT NULL,
  `job_uuid` VARCHAR(36) NOT NULL,
  `segment_uuid` VARCHAR(36) NOT NULL,
  `generation_uuid` VARCHAR(36) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `metric_uuid` VARCHAR(36) NOT NULL,
  `metric_version_uuid` VARCHAR(36) NOT NULL,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `dimension_set_code` VARCHAR(64) NOT NULL DEFAULT 'BASE',
  `grain_type` VARCHAR(30) NOT NULL,
  `entity_uuid` VARCHAR(36) NOT NULL DEFAULT '',
  `dimension_hash` CHAR(64) NOT NULL,
  `dimension_json` JSON NOT NULL,
  `metric_value` DECIMAL(24,6) NOT NULL,
  `source_as_of` DATETIME NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_value_stage_grain`
    (`job_uuid`, `segment_uuid`, `metric_version_uuid`, `dimension_set_code`, `grain_type`, `dimension_hash`, `entity_uuid`),
  KEY `idx_metric_value_stage_publish` (`segment_uuid`, `generation_uuid`, `period_start`, `uuid`),
  CONSTRAINT `fk_metric_value_stage_job` FOREIGN KEY (`job_uuid`)
    REFERENCES `rpt_metric_materialization_job` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_value_stage_segment` FOREIGN KEY (`segment_uuid`)
    REFERENCES `rpt_metric_materialization_segment` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_value_stage_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_value_stage_version` FOREIGN KEY (`metric_version_uuid`, `metric_uuid`)
    REFERENCES `rpt_metric_version` (`uuid`, `metric_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_metric_value_stage_period` CHECK (`period_end` >= `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标物化暂存值';

CREATE TABLE `rpt_metric_value` (
  `period_start` DATE NOT NULL,
  `uuid` VARCHAR(36) NOT NULL,
  `generation_uuid` VARCHAR(36) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `metric_uuid` VARCHAR(36) NOT NULL,
  `metric_version_uuid` VARCHAR(36) NOT NULL,
  `period_end` DATE NOT NULL,
  `dimension_set_code` VARCHAR(64) NOT NULL DEFAULT 'BASE',
  `grain_type` VARCHAR(30) NOT NULL,
  `entity_uuid` VARCHAR(36) NOT NULL DEFAULT '',
  `dimension_hash` CHAR(64) NOT NULL,
  `dimension_json` JSON NOT NULL,
  `metric_value` DECIMAL(24,6) NOT NULL,
  `source_as_of` DATETIME NOT NULL,
  `published_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`period_start`, `uuid`),
  UNIQUE KEY `uk_metric_value_generation_grain`
    (`period_start`, `generation_uuid`, `metric_version_uuid`, `dimension_set_code`, `grain_type`, `dimension_hash`, `entity_uuid`),
  KEY `idx_metric_value_query` (`period_start`, `metric_version_uuid`, `dimension_hash`, `entity_uuid`),
  KEY `idx_metric_value_generation` (`period_start`, `generation_uuid`, `uuid`),
  CONSTRAINT `chk_metric_value_period` CHECK (`period_end` >= `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐指标版本长表值'
PARTITION BY RANGE COLUMNS (`period_start`) (
  PARTITION `p_before_2024` VALUES LESS THAN ('2024-01-01'),
  PARTITION `p2024q1` VALUES LESS THAN ('2024-04-01'),
  PARTITION `p2024q2` VALUES LESS THAN ('2024-07-01'),
  PARTITION `p2024q3` VALUES LESS THAN ('2024-10-01'),
  PARTITION `p2024q4` VALUES LESS THAN ('2025-01-01'),
  PARTITION `p2025q1` VALUES LESS THAN ('2025-04-01'),
  PARTITION `p2025q2` VALUES LESS THAN ('2025-07-01'),
  PARTITION `p2025q3` VALUES LESS THAN ('2025-10-01'),
  PARTITION `p2025q4` VALUES LESS THAN ('2026-01-01'),
  PARTITION `p2026q1` VALUES LESS THAN ('2026-04-01'),
  PARTITION `p2026q2` VALUES LESS THAN ('2026-07-01'),
  PARTITION `p2026q3` VALUES LESS THAN ('2026-10-01'),
  PARTITION `p2026q4` VALUES LESS THAN ('2027-01-01'),
  PARTITION `p2027q1` VALUES LESS THAN ('2027-04-01'),
  PARTITION `p2027q2` VALUES LESS THAN ('2027-07-01'),
  PARTITION `p2027q3` VALUES LESS THAN ('2027-10-01'),
  PARTITION `p2027q4` VALUES LESS THAN ('2028-01-01'),
  PARTITION `p2028q1` VALUES LESS THAN ('2028-04-01'),
  PARTITION `p2028q2` VALUES LESS THAN ('2028-07-01'),
  PARTITION `p2028q3` VALUES LESS THAN ('2028-10-01'),
  PARTITION `p2028q4` VALUES LESS THAN ('2029-01-01'),
  PARTITION `p_future` VALUES LESS THAN (MAXVALUE)
);

CREATE TABLE `rpt_report_snapshot` (
  `uuid` VARCHAR(36) NOT NULL,
  `snapshot_key` CHAR(64) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `report_code` VARCHAR(64) NOT NULL,
  `query_hash` CHAR(64) NOT NULL,
  `query_json` JSON NOT NULL,
  `payload_json` JSON NOT NULL,
  `source_as_of` DATETIME NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `snapshot_status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_snapshot_key` (`snapshot_key`),
  KEY `idx_report_snapshot_lookup` (`metric_release_uuid`, `report_code`, `query_hash`, `snapshot_status`),
  KEY `idx_report_snapshot_cleanup` (`snapshot_status`, `expires_at`, `uuid`),
  CONSTRAINT `fk_report_snapshot_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_snapshot_status` CHECK (`snapshot_status` IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可过期报表服务快照';

CREATE TABLE `rpt_report_snapshot_reference` (
  `uuid` VARCHAR(36) NOT NULL,
  `snapshot_uuid` VARCHAR(36) NOT NULL,
  `reference_type` VARCHAR(30) NOT NULL,
  `reference_uuid` VARCHAR(36) NOT NULL,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_snapshot_reference` (`snapshot_uuid`, `reference_type`, `reference_uuid`),
  KEY `idx_report_snapshot_reference_source` (`reference_type`, `reference_uuid`),
  CONSTRAINT `fk_report_snapshot_reference_snapshot` FOREIGN KEY (`snapshot_uuid`)
    REFERENCES `rpt_report_snapshot` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_snapshot_reference_type` CHECK (`reference_type` IN ('FIXED_VIEW', 'AUDIT_REPORT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表快照保留引用';

CREATE TABLE `rpt_report_saved_view` (
  `uuid` VARCHAR(36) NOT NULL,
  `owner_uuid` VARCHAR(36) NOT NULL,
  `view_name` VARCHAR(100) NOT NULL,
  `report_path` VARCHAR(80) NOT NULL,
  `query_json` JSON NOT NULL,
  `dimension_code` VARCHAR(20) NULL,
  `metric_codes_json` JSON NOT NULL,
  `is_default` TINYINT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(100) NULL,
  `update_by` VARCHAR(100) NULL,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `version` INT NOT NULL DEFAULT 1,
  `active_name` VARCHAR(100) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `view_name` ELSE NULL END
  ) STORED,
  `active_default_path` VARCHAR(80) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 AND `is_default` = 1 THEN `report_path` ELSE NULL END
  ) STORED,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_saved_view_owner_name` (`owner_uuid`, `active_name`),
  UNIQUE KEY `uk_report_saved_view_owner_default` (`owner_uuid`, `active_default_path`),
  KEY `idx_report_saved_view_owner` (`owner_uuid`, `is_deleted`, `update_time`, `uuid`),
  CONSTRAINT `fk_report_saved_view_owner` FOREIGN KEY (`owner_uuid`)
    REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_saved_view_default` CHECK (`is_default` IN (0, 1)),
  CONSTRAINT `chk_report_saved_view_deleted` CHECK (`is_deleted` IN (0, 1)),
  CONSTRAINT `chk_report_saved_view_version` CHECK (`version` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人报表保存视图';

-- Customer-facing finish revisions. Physical finish fields remain authoritative.
DROP TABLE IF EXISTS `biz_delivery_customer_revision_item`;
DROP TABLE IF EXISTS `biz_delivery_customer_revision`;
DROP TABLE IF EXISTS `biz_finish_customer_revision_item`;
DROP TABLE IF EXISTS `biz_finish_customer_revision`;

CREATE TABLE `biz_finish_customer_revision` (
  `uuid` VARCHAR(36) NOT NULL,
  `order_uuid` VARCHAR(36) NOT NULL,
  `revision_no` INT NOT NULL,
  `request_id` VARCHAR(64) NOT NULL,
  `request_hash` CHAR(64) NOT NULL,
  `source_stage` VARCHAR(20) NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `item_count` INT NOT NULL,
  `customer_total_weight` DECIMAL(14,3) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_finish_customer_revision_no` (`order_uuid`,`revision_no`),
  UNIQUE KEY `uk_finish_customer_revision_request` (`order_uuid`,`request_id`),
  KEY `idx_finish_customer_revision_history` (`order_uuid`,`is_deleted`,`revision_no`),
  CONSTRAINT `fk_finish_customer_revision_order` FOREIGN KEY (`order_uuid`)
    REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_finish_customer_revision_no` CHECK (`revision_no` >= 1),
  CONSTRAINT `chk_finish_customer_revision_count` CHECK (`item_count` >= 1),
  CONSTRAINT `chk_finish_customer_revision_total` CHECK (`customer_total_weight` IS NULL OR `customer_total_weight` > 0),
  CONSTRAINT `chk_finish_customer_revision_deleted` CHECK (`is_deleted` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加工单客户口径版本';

CREATE TABLE `biz_finish_customer_revision_item` (
  `uuid` VARCHAR(36) NOT NULL,
  `revision_uuid` VARCHAR(36) NOT NULL,
  `finish_uuid` VARCHAR(36) NOT NULL,
  `physical_paper_name` VARCHAR(100) NOT NULL,
  `physical_gram_weight` INT NOT NULL,
  `physical_finish_width` INT NOT NULL,
  `physical_weight_snapshot` DECIMAL(12,3) DEFAULT NULL,
  `customer_paper_name` VARCHAR(100) NOT NULL,
  `customer_gram_weight` INT NOT NULL,
  `customer_finish_width` INT NOT NULL,
  `customer_display_weight` DECIMAL(12,3) DEFAULT NULL,
  `calculation_mode` VARCHAR(16) NOT NULL,
  `weight_operand` DECIMAL(20,6) DEFAULT NULL,
  `formula_expression` VARCHAR(500) DEFAULT NULL,
  `formula_inputs` JSON DEFAULT NULL,
  `rounding_scale` TINYINT NOT NULL DEFAULT 3,
  `rounding_mode` VARCHAR(16) NOT NULL DEFAULT 'HALF_UP',
  `zero_policy` VARCHAR(16) NOT NULL DEFAULT 'SKIP',
  `remark` VARCHAR(255) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_finish_customer_revision_item` (`revision_uuid`,`finish_uuid`),
  KEY `idx_finish_customer_revision_item_finish` (`finish_uuid`,`revision_uuid`),
  CONSTRAINT `fk_finish_customer_revision_item_revision` FOREIGN KEY (`revision_uuid`)
    REFERENCES `biz_finish_customer_revision` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_finish_customer_revision_item_finish` FOREIGN KEY (`finish_uuid`)
    REFERENCES `biz_finish_roll` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_finish_customer_item_physical` CHECK (`physical_gram_weight` > 0 AND `physical_finish_width` > 0),
  CONSTRAINT `chk_finish_customer_item_customer` CHECK (`customer_gram_weight` > 0 AND `customer_finish_width` > 0),
  CONSTRAINT `chk_finish_customer_item_weight` CHECK (`customer_display_weight` IS NULL OR `customer_display_weight` > 0),
  CONSTRAINT `chk_finish_customer_item_mode` CHECK (`calculation_mode` IN ('KEEP','FIXED','DELTA','RATIO','FORMULA','MANUAL')),
  CONSTRAINT `chk_finish_customer_item_rounding` CHECK (`rounding_scale` BETWEEN 0 AND 3 AND `rounding_mode` IN ('HALF_UP','UP','DOWN')),
  CONSTRAINT `chk_finish_customer_item_zero` CHECK (`zero_policy` IN ('SKIP','ERROR','USE_ZERO')),
  CONSTRAINT `chk_finish_customer_item_deleted` CHECK (`is_deleted` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加工单客户口径逐卷明细';

CREATE TABLE `biz_delivery_customer_revision` (
  `uuid` VARCHAR(36) NOT NULL,
  `delivery_uuid` VARCHAR(36) NOT NULL,
  `revision_no` INT NOT NULL,
  `request_id` VARCHAR(64) NOT NULL,
  `request_hash` CHAR(64) NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `item_count` INT NOT NULL,
  `customer_total_weight` DECIMAL(14,3) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_delivery_customer_revision_no` (`delivery_uuid`,`revision_no`),
  UNIQUE KEY `uk_delivery_customer_revision_request` (`delivery_uuid`,`request_id`),
  KEY `idx_delivery_customer_revision_history` (`delivery_uuid`,`is_deleted`,`revision_no`),
  CONSTRAINT `fk_delivery_customer_revision_order` FOREIGN KEY (`delivery_uuid`)
    REFERENCES `biz_delivery_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_delivery_customer_revision_no` CHECK (`revision_no` >= 1),
  CONSTRAINT `chk_delivery_customer_revision_count` CHECK (`item_count` >= 1),
  CONSTRAINT `chk_delivery_customer_revision_total` CHECK (`customer_total_weight` IS NULL OR `customer_total_weight` > 0),
  CONSTRAINT `chk_delivery_customer_revision_deleted` CHECK (`is_deleted` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单客户更正版';

CREATE TABLE `biz_delivery_customer_revision_item` (
  `uuid` VARCHAR(36) NOT NULL,
  `revision_uuid` VARCHAR(36) NOT NULL,
  `delivery_detail_uuid` VARCHAR(36) NOT NULL,
  `finish_uuid` VARCHAR(36) NOT NULL,
  `physical_paper_name` VARCHAR(100) NOT NULL,
  `physical_gram_weight` INT NOT NULL,
  `physical_finish_width` INT NOT NULL,
  `physical_delivery_weight` DECIMAL(12,3) NOT NULL,
  `customer_paper_name` VARCHAR(100) NOT NULL,
  `customer_gram_weight` INT NOT NULL,
  `customer_finish_width` INT NOT NULL,
  `customer_display_weight` DECIMAL(12,3) NOT NULL,
  `calculation_mode` VARCHAR(16) NOT NULL,
  `weight_operand` DECIMAL(20,6) DEFAULT NULL,
  `formula_expression` VARCHAR(500) DEFAULT NULL,
  `formula_inputs` JSON DEFAULT NULL,
  `rounding_scale` TINYINT NOT NULL DEFAULT 3,
  `rounding_mode` VARCHAR(16) NOT NULL DEFAULT 'HALF_UP',
  `zero_policy` VARCHAR(16) NOT NULL DEFAULT 'SKIP',
  `customer_remark` VARCHAR(255) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_delivery_customer_revision_item` (`revision_uuid`,`delivery_detail_uuid`),
  KEY `idx_delivery_customer_revision_item_detail` (`delivery_detail_uuid`,`revision_uuid`),
  KEY `idx_delivery_customer_revision_item_finish` (`finish_uuid`,`revision_uuid`),
  CONSTRAINT `fk_delivery_customer_item_revision` FOREIGN KEY (`revision_uuid`)
    REFERENCES `biz_delivery_customer_revision` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_delivery_customer_item_detail` FOREIGN KEY (`delivery_detail_uuid`)
    REFERENCES `biz_delivery_detail` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_delivery_customer_item_finish` FOREIGN KEY (`finish_uuid`)
    REFERENCES `biz_finish_roll` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_delivery_customer_item_physical` CHECK (`physical_gram_weight` > 0 AND `physical_finish_width` > 0 AND `physical_delivery_weight` > 0),
  CONSTRAINT `chk_delivery_customer_item_customer` CHECK (`customer_gram_weight` > 0 AND `customer_finish_width` > 0 AND `customer_display_weight` > 0),
  CONSTRAINT `chk_delivery_customer_item_mode` CHECK (`calculation_mode` IN ('KEEP','FIXED','DELTA','RATIO','FORMULA','MANUAL')),
  CONSTRAINT `chk_delivery_customer_item_rounding` CHECK (`rounding_scale` BETWEEN 0 AND 3 AND `rounding_mode` IN ('HALF_UP','UP','DOWN')),
  CONSTRAINT `chk_delivery_customer_item_zero` CHECK (`zero_policy` IN ('SKIP','ERROR','USE_ZERO')),
  CONSTRAINT `chk_delivery_customer_item_deleted` CHECK (`is_deleted` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单客户更正版逐卷明细';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 建表脚本结束  共 48 张表
--   基础档案 4: sys_customer / sys_paper / sys_machine / sys_warehouse
--   加工核心 8: biz_process_order / biz_original_roll / biz_process_step /
--               biz_process_stage_output / biz_process_stage_input_rel /
--               biz_finish_roll / biz_process_param /
--               biz_finish_original_rel
--   出库     2: biz_delivery_order / biz_delivery_detail
--   结算收款 4: biz_settle_order / biz_settle_detail / biz_receive_record / biz_settle_collection_reminder
--   工艺草稿 2: biz_process_config_draft / sys_roll_no_sequence
--   系统辅助 9: sys_operation_log / sys_user / sys_user_session / sys_no_rule /
--               sys_dict_item / sys_config_item / sys_backup_task / sys_notification / sys_export_task
--   报表语义 4: rpt_metric_definition / rpt_metric_version /
--               rpt_metric_release / rpt_metric_release_item
--   报表订阅 3: rpt_report_subscription / rpt_report_subscription_recipient /
--               rpt_report_subscription_run
--   报表告警 3: rpt_alert_signal_definition / rpt_alert_rule / rpt_alert_event
--   报表物化 7: rpt_metric_materialization_job / rpt_metric_materialization_segment /
--               rpt_metric_materialization_state / rpt_metric_value_stage / rpt_metric_value /
--               rpt_report_snapshot / rpt_report_snapshot_reference
-- =============================================================================
