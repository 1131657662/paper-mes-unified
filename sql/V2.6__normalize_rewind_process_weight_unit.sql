UPDATE biz_process_step ps
SET ps.process_weight = ROUND(ps.process_weight / 1000, 3)
WHERE ps.is_deleted = 0
  AND ps.step_type = 2
  AND ps.process_weight >= 100
  AND COALESCE(ps.unit_price, 0) > 0
  AND ABS(COALESCE(ps.step_amount, 0)
    - ROUND(ps.process_weight / 1000 * ps.unit_price, 2)) <= 1;
