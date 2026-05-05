SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_analysis_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `analysis_type` VARCHAR(64) NOT NULL COMMENT '分析类型',
  `source_report_id` BIGINT UNSIGNED NULL COMMENT '关联报告ID',
  `provider` VARCHAR(64) NOT NULL COMMENT '模型提供方',
  `model` VARCHAR(128) NOT NULL COMMENT '模型名称',
  `mocked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否Mock:0否,1是',
  `success` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否成功:0否,1是',
  `latency_ms` BIGINT NOT NULL DEFAULT 0 COMMENT '调用耗时(毫秒)',
  `input_summary` TEXT NULL COMMENT '输入摘要',
  `output_content` LONGTEXT NULL COMMENT '输出内容(JSON)',
  `error_message` TEXT NULL COMMENT '错误信息',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_analysis_type` (`analysis_type`),
  KEY `idx_source_report_id` (`source_report_id`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_ai_analysis_record_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分析记录表';
