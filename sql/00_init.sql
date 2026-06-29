-- =============================================================================
-- 数据库初始化脚本
-- 用法（在仓库根目录依次执行两条命令）：
--   mysql -u root -p < sql/00_init.sql
--   mysql -u root -p paper_processing < sql/01_schema_v4.1.sql
-- 作用：第 1 条创建 paper_processing 库（utf8mb4）；第 2 条建 16 张表。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `paper_processing`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
