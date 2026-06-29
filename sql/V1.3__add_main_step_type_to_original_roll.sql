-- V1.3: 为原纸表添加主工艺类型字段
-- 用于在创建加工单时记录每个原纸卷的主工艺类型（锯纸/复卷）

ALTER TABLE `biz_original_roll`
ADD COLUMN `main_step_type` TINYINT DEFAULT NULL COMMENT '主工艺类型：1锯纸 2复卷（标准加工和现场定尺必填）'
AFTER `process_mode`;

-- 添加索引以便按工艺类型查询
CREATE INDEX `idx_main_step_type` ON `biz_original_roll`(`main_step_type`);
