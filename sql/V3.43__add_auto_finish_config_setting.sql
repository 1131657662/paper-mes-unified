INSERT INTO `sys_config_item`
(`uuid`, `config_group`, `config_key`, `config_name`, `config_value`, `value_type`,
 `unit`, `sort_no`, `status`, `built_in`, `remark`, `create_by`, `update_by`)
SELECT
  'cfg-auto-finish-config', 'process', 'process.autoFinishConfig', '成品配置允许自动生成',
  'false', 'boolean', NULL, 5, 1, 1,
  '开启后可生成默认成品配置，但提交前仍须人工确认', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_config_item`
  WHERE `config_key` = 'process.autoFinishConfig' AND `is_deleted` = 0
);
