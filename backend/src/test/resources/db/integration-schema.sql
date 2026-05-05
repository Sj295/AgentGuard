DROP TABLE IF EXISTS risk_report;
DROP TABLE IF EXISTS ai_analysis_record;
DROP TABLE IF EXISTS agent_rule;
DROP TABLE IF EXISTS scan_result;
DROP TABLE IF EXISTS scan_task;
DROP TABLE IF EXISTS project_info;

CREATE TABLE project_info (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_name VARCHAR(128) NOT NULL,
  project_path VARCHAR(512) NOT NULL,
  project_type VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
  tech_stack CLOB,
  has_git BOOLEAN NOT NULL DEFAULT FALSE,
  has_agents_md BOOLEAN NOT NULL DEFAULT FALSE,
  description VARCHAR(500),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_project_info_type ON project_info(project_type);
CREATE INDEX idx_project_info_created_time ON project_info(created_time);
CREATE INDEX idx_project_info_deleted ON project_info(deleted);
CREATE INDEX idx_project_info_path_deleted ON project_info(project_path, deleted);

CREATE TABLE scan_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  task_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  progress INT NOT NULL DEFAULT 0,
  result_summary VARCHAR(1000),
  error_message VARCHAR(1000),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_scan_task_project_id ON scan_task(project_id);
CREATE INDEX idx_scan_task_status ON scan_task(status);
CREATE INDEX idx_scan_task_created_time ON scan_task(created_time);
CREATE INDEX idx_scan_task_deleted ON scan_task(deleted);

CREATE TABLE scan_result (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  file_count INT NOT NULL DEFAULT 0,
  directory_count INT NOT NULL DEFAULT 0,
  detected_files CLOB,
  detected_commands CLOB,
  sensitive_files CLOB,
  risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_scan_result_project_id ON scan_result(project_id);
CREATE INDEX idx_scan_result_task_id ON scan_result(task_id);
CREATE INDEX idx_scan_result_risk_level ON scan_result(risk_level);
CREATE INDEX idx_scan_result_created_time ON scan_result(created_time);
CREATE INDEX idx_scan_result_deleted ON scan_result(deleted);

CREATE TABLE agent_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  agent_type VARCHAR(32) NOT NULL,
  file_name VARCHAR(128) NOT NULL,
  content CLOB,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_agent_rule_project_id ON agent_rule(project_id);
CREATE INDEX idx_agent_rule_agent_type ON agent_rule(agent_type);
CREATE INDEX idx_agent_rule_created_time ON agent_rule(created_time);
CREATE INDEX idx_agent_rule_deleted ON agent_rule(deleted);
CREATE INDEX idx_agent_rule_project_agent_file ON agent_rule(project_id, agent_type, file_name);

CREATE TABLE risk_report (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  report_type VARCHAR(32) NOT NULL,
  risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
  risk_score INT,
  summary VARCHAR(1000),
  risk_items CLOB,
  suggestions CLOB,
  payload_json CLOB,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_risk_report_project_id ON risk_report(project_id);
CREATE INDEX idx_risk_report_report_type ON risk_report(report_type);
CREATE INDEX idx_risk_report_risk_level ON risk_report(risk_level);
CREATE INDEX idx_risk_report_risk_score ON risk_report(risk_score);
CREATE INDEX idx_risk_report_created_time ON risk_report(created_time);
CREATE INDEX idx_risk_report_deleted ON risk_report(deleted);

CREATE TABLE ai_analysis_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  analysis_type VARCHAR(64) NOT NULL,
  source_report_id BIGINT,
  provider VARCHAR(64) NOT NULL,
  model VARCHAR(128) NOT NULL,
  mocked BOOLEAN NOT NULL DEFAULT FALSE,
  success BOOLEAN NOT NULL DEFAULT TRUE,
  latency_ms BIGINT NOT NULL DEFAULT 0,
  input_summary CLOB,
  output_content CLOB,
  error_message CLOB,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_project_id ON ai_analysis_record(project_id);
CREATE INDEX idx_analysis_type ON ai_analysis_record(analysis_type);
CREATE INDEX idx_source_report_id ON ai_analysis_record(source_report_id);
CREATE INDEX idx_created_time ON ai_analysis_record(created_time);
CREATE INDEX idx_ai_analysis_record_deleted ON ai_analysis_record(deleted);
