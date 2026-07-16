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
  KEY `idx_is_deleted` (`is_deleted`)
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
  `remark`          VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `warehouse_code_active` VARCHAR(50)
    GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`warehouse_code`), '') ELSE NULL END)
    STORED COMMENT '启用仓库唯一编码',
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
  KEY `idx_warehouse_name` (`warehouse_name`),
  KEY `idx_status` (`status`),
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
  `process_mode`       TINYINT       NOT NULL DEFAULT 1      COMMENT '1标准加工 2现场定尺 3不加工直发',
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
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原纸明细表（单卷维度）';

-- -----------------------------------------------------------------------------
-- 3.3.4 biz_process_step 工序明细表（工艺唯一来源）
-- -----------------------------------------------------------------------------
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
  `step_type`      TINYINT       NOT NULL                COMMENT '1锯纸 2复卷（工艺唯一判断字段）',
  `step_name`      VARCHAR(50)   DEFAULT NULL            COMMENT '工序自定义名称',
  `machine_uuid`   VARCHAR(36)   DEFAULT NULL            COMMENT '工序加工机台',
  `machine_name_snap` VARCHAR(100) DEFAULT NULL           COMMENT '工序机台名称快照',
  `is_main`        TINYINT       NOT NULL DEFAULT 1      COMMENT '1本卷主工艺 0车间追加工序',
  `knife_count`    INT           DEFAULT 0               COMMENT '锯纸专用：实际加工刀数',
  `process_weight` DECIMAL(10,3) DEFAULT NULL            COMMENT '复卷专用：加工吨位',
  `unit_price`     DECIMAL(10,2) DEFAULT NULL            COMMENT '本工序单价（元/刀 / 元/吨）',
  `step_amount`    DECIMAL(10,2) DEFAULT 0.00            COMMENT '本工序加工费（取整）',
  `loss_weight`    DECIMAL(10,3) DEFAULT 0.000           COMMENT '本工序产生损耗重量 kg',
  `operator`       VARCHAR(50)   DEFAULT NULL            COMMENT '本工序操作工',
  `remark`         VARCHAR(255)  DEFAULT NULL            COMMENT '工序备注、异常说明',
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
  KEY `idx_order_uuid` (`order_uuid`),
  KEY `idx_original_uuid` (`original_uuid`),
  KEY `idx_input_output_uuid` (`input_output_uuid`),
  KEY `idx_parent_step_uuid` (`parent_step_uuid`),
  KEY `idx_process_step_machine_uuid` (`machine_uuid`),
  KEY `idx_step_type` (`step_type`),
  KEY `idx_is_deleted` (`is_deleted`)
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
  `finish_diameter`      INT           DEFAULT NULL            COMMENT '成品直径 英寸',
  `finish_core_diameter` INT           DEFAULT NULL            COMMENT '成品纸芯直径 英寸',
  `source_type`          TINYINT       NOT NULL DEFAULT 1      COMMENT '1加工产出 2原纸直发(沿用母卷号,三不约束)',
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
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库成品明细表';

-- =============================================================================
-- 四、结算收款模块
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 3.3.10 biz_settle_order 结算单主表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `biz_settle_order`;
CREATE TABLE `biz_settle_order` (
  `uuid`              VARCHAR(36)   NOT NULL                COMMENT '结算单主键',
  `settle_no`         VARCHAR(50)   NOT NULL                COMMENT '结算单号',
  `customer_uuid`     VARCHAR(36)   NOT NULL                COMMENT '关联客户',
  `customer_name`     VARCHAR(100)  NOT NULL                COMMENT '快照冗余客户名',
  `settle_type`       TINYINT       NOT NULL DEFAULT 1      COMMENT '1按单 2按月批量',
  `settle_date`       DATE          NOT NULL                COMMENT '结算日期',
  `period_start`      DATE          DEFAULT NULL            COMMENT '月结账期起',
  `period_end`        DATE          DEFAULT NULL            COMMENT '月结账期止',
  `saw_amount`        DECIMAL(12,2) DEFAULT 0.00            COMMENT '锯纸加工费合计',
  `rewind_amount`     DECIMAL(12,2) DEFAULT 0.00            COMMENT '复卷加工费合计',
  `extra_amount`      DECIMAL(12,2) DEFAULT 0.00            COMMENT '附加费合计',
  `amount_no_tax`     DECIMAL(12,2) DEFAULT 0.00            COMMENT '不含税金额',
  `tax_amount`        DECIMAL(12,2) DEFAULT 0.00            COMMENT '税额',
  `total_amount`      DECIMAL(12,2) NOT NULL DEFAULT 0.00  COMMENT '应收总金额（取整）',
  `received_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0.00  COMMENT '已结清金额',
  `cash_received_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '现金实收金额',
  `scrap_offset_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '废纸抵扣金额',
  `discount_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0.00  COMMENT '优惠及尾差核销金额',
  `unreceived_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00  COMMENT '待收金额',
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
  KEY `idx_customer_uuid` (`customer_uuid`),
  KEY `idx_settle_status` (`settle_status`),
  KEY `idx_settle_date` (`settle_date`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `chk_settle_discount_nonnegative` CHECK (`discount_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算单主表';

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
  `receive_date`   DATETIME      NOT NULL                COMMENT '收款时间',
  `receive_amount` DECIMAL(12,2) NOT NULL                COMMENT '本次结清金额',
  `cash_amount`    DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '现金实收金额',
  `scrap_offset_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '废纸抵扣金额',
  `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠及尾差核销金额',
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
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `chk_receive_discount_nonnegative` CHECK (`discount_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分次收款流水表';

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

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 建表脚本结束  共 27 张表
--   基础档案 4: sys_customer / sys_paper / sys_machine / sys_warehouse
--   加工核心 8: biz_process_order / biz_original_roll / biz_process_step /
--               biz_process_stage_output / biz_process_stage_input_rel /
--               biz_finish_roll / biz_process_param /
--               biz_finish_original_rel
--   出库     2: biz_delivery_order / biz_delivery_detail
--   结算收款 3: biz_settle_order / biz_settle_detail / biz_receive_record
--   工艺草稿 2: biz_process_config_draft / sys_roll_no_sequence
--   系统辅助 8: sys_operation_log / sys_user / sys_user_session / sys_no_rule /
--               sys_dict_item / sys_config_item / sys_backup_task / sys_notification
-- =============================================================================
