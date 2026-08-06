-- V3.62: cash_received_amount stores actual received funds for every payment channel.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

UPDATE `rpt_metric_definition`
SET `metric_name` = '实际到账',
    `description` = '有效收款流水中的实际到账金额，包含现金、转账、微信和支付宝',
    `update_by` = 'system',
    `version` = `version` + 1
WHERE `metric_code` = 'cash_received_amount'
  AND (`metric_name` <> '实际到账'
    OR `description` <> '有效收款流水中的实际到账金额，包含现金、转账、微信和支付宝');
