-- =============================================================
-- P3-3 性能优化：高频查询索引迁移
-- 数据库：paper_processing（MySQL 8.0）
-- 说明：MySQL 8 不支持 CREATE INDEX IF NOT EXISTS，本脚本仅首次执行；
--      重复执行会报「Duplicate key name」，可忽略。
-- 范围：仅新增索引，不改字段类型/表结构（不违反「不改字段 DDL」）。
-- =============================================================

-- create_time 排序索引：所有列表分页均 ORDER BY create_time DESC，
-- 无对应索引会触发 filesort。
CREATE INDEX idx_create_time ON biz_process_order (create_time);
CREATE INDEX idx_create_time ON biz_delivery_order (create_time);
CREATE INDEX idx_create_time ON biz_settle_order (create_time);
CREATE INDEX idx_create_time ON sys_customer (create_time);
CREATE INDEX idx_create_time ON sys_paper (create_time);
CREATE INDEX idx_create_time ON sys_machine (create_time);
CREATE INDEX idx_create_time ON sys_warehouse (create_time);

-- 详情/可用成品排序复合索引：工序明细按 step_sort、成品明细按 row_sort 排序。
CREATE INDEX idx_order_step_sort ON biz_process_step (order_uuid, step_sort);
CREATE INDEX idx_order_row_sort ON biz_finish_roll (order_uuid, row_sort);

-- 结算关联加工单高频组合：客户 + 已完成(order_status=4) + 日期范围 + 排序。
CREATE INDEX idx_cust_status_ctime ON biz_process_order (customer_uuid, order_status, create_time);
