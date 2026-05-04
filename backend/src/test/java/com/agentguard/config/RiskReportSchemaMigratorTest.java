package com.agentguard.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskReportSchemaMigratorTest {

    @Test
    void migrateRiskReportSchema_whenColumnsMissing_shouldAddColumnsAndIndex() throws SQLException {
        JdbcTemplate jdbcTemplate = jdbcTemplateWithSchema(
                true,
                Set.of(),
                Set.of()
        );

        new RiskReportSchemaMigrator(jdbcTemplate).migrateRiskReportSchema();

        verify(jdbcTemplate, times(4)).update(anyString());
    }

    @Test
    void migrateRiskReportSchema_whenSchemaExists_shouldNotAlterTable() throws SQLException {
        JdbcTemplate jdbcTemplate = jdbcTemplateWithSchema(
                true,
                Set.of("risk_score", "summary", "payload_json"),
                Set.of("idx_risk_report_risk_score")
        );

        new RiskReportSchemaMigrator(jdbcTemplate).migrateRiskReportSchema();

        verify(jdbcTemplate, never()).update(anyString());
    }

    @Test
    void migrateRiskReportSchema_whenTableMissing_shouldSkip() throws SQLException {
        JdbcTemplate jdbcTemplate = jdbcTemplateWithSchema(
                false,
                Set.of(),
                Set.of()
        );

        new RiskReportSchemaMigrator(jdbcTemplate).migrateRiskReportSchema();

        verify(jdbcTemplate, never()).update(anyString());
    }

    private JdbcTemplate jdbcTemplateWithSchema(boolean hasRiskReportTable,
                                                Set<String> existingColumns,
                                                Set<String> existingIndexes) throws SQLException {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);

        when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.getCatalog()).thenReturn("agentguard");
        when(metadata.getDatabaseProductName()).thenReturn("MySQL");

        when(metadata.getTables(any(), any(), anyString(), any()))
                .thenAnswer(invocation -> resultSet(hasRiskReportTable
                        && "risk_report".equalsIgnoreCase(invocation.getArgument(2)), null));
        when(metadata.getColumns(any(), any(), anyString(), anyString()))
                .thenAnswer(invocation -> resultSet(existingColumns.stream()
                        .anyMatch(column -> column.equalsIgnoreCase(invocation.getArgument(3))), null));
        when(metadata.getIndexInfo(any(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation -> {
                    boolean hasIndex = existingIndexes.stream()
                            .anyMatch(index -> "idx_risk_report_risk_score".equalsIgnoreCase(index));
                    return resultSet(hasIndex, "idx_risk_report_risk_score");
                });

        return jdbcTemplate;
    }

    private ResultSet resultSet(boolean hasRow, String indexName) throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(hasRow, false);
        when(resultSet.getString("INDEX_NAME")).thenReturn(indexName);
        return resultSet;
    }
}
