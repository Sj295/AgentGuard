SET NAMES utf8mb4;

DROP TABLE IF EXISTS `risk_report`;
DROP TABLE IF EXISTS `agent_rule`;
DROP TABLE IF EXISTS `scan_result`;
DROP TABLE IF EXISTS `scan_task`;
DROP TABLE IF EXISTS `project_info`;

CREATE TABLE `project_info` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_name` VARCHAR(128) NOT NULL COMMENT '项目名称',
  `project_path` VARCHAR(512) NOT NULL COMMENT '项目本地路径',
  `project_type` VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN' COMMENT '项目类型',
  `tech_stack` TEXT NULL COMMENT '技术栈(JSON字符串)',
  `has_git` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否存在Git仓库',
  `has_agents_md` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否存在AGENTS.md',
  `description` VARCHAR(500) NULL COMMENT '项目描述',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_project_info_type` (`project_type`),
  KEY `idx_project_info_created_time` (`created_time`),
  KEY `idx_project_info_deleted` (`deleted`),
  KEY `idx_project_info_path_deleted` (`project_path`(255), `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目基本信息表';

CREATE TABLE `scan_task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `task_type` VARCHAR(32) NOT NULL COMMENT '任务类型',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
  `progress` INT NOT NULL DEFAULT 0 COMMENT '任务进度(0-100)',
  `result_summary` VARCHAR(1000) NULL COMMENT '任务结果摘要',
  `error_message` VARCHAR(1000) NULL COMMENT '错误信息',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_scan_task_project_id` (`project_id`),
  KEY `idx_scan_task_status` (`status`),
  KEY `idx_scan_task_created_time` (`created_time`),
  KEY `idx_scan_task_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扫描任务表';

CREATE TABLE `scan_result` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `task_id` BIGINT UNSIGNED NOT NULL COMMENT '扫描任务ID',
  `file_count` INT NOT NULL DEFAULT 0 COMMENT '文件数量',
  `directory_count` INT NOT NULL DEFAULT 0 COMMENT '目录数量',
  `detected_files` TEXT NULL COMMENT '识别到的关键文件(JSON字符串)',
  `detected_commands` TEXT NULL COMMENT '识别到的命令(JSON字符串)',
  `sensitive_files` TEXT NULL COMMENT '敏感文件列表(JSON字符串)',
  `risk_level` VARCHAR(32) NOT NULL DEFAULT 'LOW' COMMENT '风险等级',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_scan_result_project_id` (`project_id`),
  KEY `idx_scan_result_task_id` (`task_id`),
  KEY `idx_scan_result_risk_level` (`risk_level`),
  KEY `idx_scan_result_created_time` (`created_time`),
  KEY `idx_scan_result_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目扫描结果表';

CREATE TABLE `agent_rule` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `agent_type` VARCHAR(32) NOT NULL COMMENT 'Agent类型',
  `file_name` VARCHAR(128) NOT NULL COMMENT '规则文件名',
  `content` TEXT NULL COMMENT '规则内容',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_agent_rule_project_id` (`project_id`),
  KEY `idx_agent_rule_agent_type` (`agent_type`),
  KEY `idx_agent_rule_created_time` (`created_time`),
  KEY `idx_agent_rule_deleted` (`deleted`),
  KEY `idx_agent_rule_project_agent_file` (`project_id`, `agent_type`, `file_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent规则文件表';

CREATE TABLE `risk_report` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `report_type` VARCHAR(32) NOT NULL COMMENT '报告类型',
  `risk_level` VARCHAR(32) NOT NULL DEFAULT 'LOW' COMMENT '风险等级',
  `risk_score` INT NULL COMMENT '风险分数(0-100)',
  `summary` VARCHAR(1000) NULL COMMENT '报告摘要',
  `risk_items` TEXT NULL COMMENT '风险项(JSON字符串)',
  `suggestions` TEXT NULL COMMENT '建议(JSON字符串)',
  `payload_json` LONGTEXT NULL COMMENT '结构化报告载荷(JSON字符串)',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_risk_report_project_id` (`project_id`),
  KEY `idx_risk_report_report_type` (`report_type`),
  KEY `idx_risk_report_risk_level` (`risk_level`),
  KEY `idx_risk_report_risk_score` (`risk_score`),
  KEY `idx_risk_report_created_time` (`created_time`),
  KEY `idx_risk_report_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风险报告表';
