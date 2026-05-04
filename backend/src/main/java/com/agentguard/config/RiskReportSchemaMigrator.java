package com.agentguard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class RiskReportSchemaMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RiskReportSchemaMigrator.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public RiskReportSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = jdbcTemplate.getDataSource();
    }

    @Override
    public void run(ApplicationArguments args) {
        migrateRiskReportSchema();
    }

    void migrateRiskReportSchema() {
        try {
            if (!isMysqlCompatibleDatabase()) {
                log.info("Skip risk_report schema migration for non-MySQL database.");
                return;
            }
            if (!tableExists("risk_report")) {
                log.warn("Skip risk_report schema migration because table does not exist.");
                return;
            }
            addColumnIfMissing("risk_score",
                    "ALTER TABLE risk_report ADD COLUMN risk_score INT NULL COMMENT '风险分数(0-100)' AFTER risk_level");
            addColumnIfMissing("summary",
                    "ALTER TABLE risk_report ADD COLUMN summary VARCHAR(1000) NULL COMMENT '报告摘要' AFTER risk_score");
            addColumnIfMissing("payload_json",
                    "ALTER TABLE risk_report ADD COLUMN payload_json LONGTEXT NULL COMMENT '结构化报告载荷(JSON字符串)' AFTER suggestions");
            addIndexIfMissing("idx_risk_report_risk_score",
                    "ALTER TABLE risk_report ADD KEY idx_risk_report_risk_score (risk_score)");
        } catch (Exception exception) {
            log.warn("Risk report schema migration skipped: {}", exception.getMessage());
        }
    }

    private boolean isMysqlCompatibleDatabase() throws SQLException {
        try (Connection connection = getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            if (databaseProductName == null) {
                return false;
            }
            String normalizedName = databaseProductName.toLowerCase(Locale.ROOT);
            return normalizedName.contains("mysql") || normalizedName.contains("mariadb");
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection connection = getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            for (String tableCandidate : nameCandidates(tableName)) {
                try (ResultSet resultSet = metadata.getTables(connection.getCatalog(), null, tableCandidate, new String[]{"TABLE"})) {
                    if (resultSet.next()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private void addColumnIfMissing(String columnName, String alterSql) throws SQLException {
        if (!columnExists("risk_report", columnName)) {
            jdbcTemplate.update(alterSql);
            log.info("Added missing risk_report column: {}", columnName);
        }
    }

    private void addIndexIfMissing(String indexName, String alterSql) throws SQLException {
        if (!indexExists("risk_report", indexName)) {
            jdbcTemplate.update(alterSql);
            log.info("Added missing risk_report index: {}", indexName);
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        try (Connection connection = getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            for (String tableCandidate : nameCandidates(tableName)) {
                for (String columnCandidate : nameCandidates(columnName)) {
                    try (ResultSet resultSet = metadata.getColumns(connection.getCatalog(), null, tableCandidate, columnCandidate)) {
                        if (resultSet.next()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private boolean indexExists(String tableName, String indexName) throws SQLException {
        try (Connection connection = getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            for (String tableCandidate : nameCandidates(tableName)) {
                try (ResultSet resultSet = metadata.getIndexInfo(connection.getCatalog(), null, tableCandidate, false, false)) {
                    while (resultSet.next()) {
                        String existingName = resultSet.getString("INDEX_NAME");
                        if (existingName != null && existingName.equalsIgnoreCase(indexName)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not available");
        }
        return dataSource.getConnection();
    }

    private Set<String> nameCandidates(String name) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(name);
        candidates.add(name.toUpperCase());
        candidates.add(name.toLowerCase());
        return candidates;
    }
}
