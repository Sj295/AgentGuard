ALTER TABLE `risk_report`
  ADD COLUMN `risk_score` INT NULL COMMENT '风险分数(0-100)' AFTER `risk_level`,
  ADD COLUMN `summary` VARCHAR(1000) NULL COMMENT '报告摘要' AFTER `risk_score`,
  ADD COLUMN `payload_json` LONGTEXT NULL COMMENT '结构化报告载荷(JSON字符串)' AFTER `suggestions`,
  ADD KEY `idx_risk_report_risk_score` (`risk_score`);
