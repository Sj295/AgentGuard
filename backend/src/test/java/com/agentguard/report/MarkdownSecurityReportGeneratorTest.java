package com.agentguard.report;

import com.agentguard.entity.AgentRule;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.entity.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownSecurityReportGeneratorTest {

    private MarkdownSecurityReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MarkdownSecurityReportGenerator();
    }

    @Test
    void generate_withFullData_shouldContainAllSections() {
        MarkdownReportContext context = buildFullContext();

        String markdown = generator.generate(context, true, true, true, true, true);

        assertNotNull(markdown);
        assertTrue(markdown.contains("# TestProject Security Report"));
        assertTrue(markdown.contains("## 1. Project Overview"));
        assertTrue(markdown.contains("## 2. Latest Scan Summary"));
        assertTrue(markdown.contains("## 3. Agent Rule Status"));
        assertTrue(markdown.contains("## 4. Risk Report Summary"));
        assertTrue(markdown.contains("## 5. Permission Assessment"));
        assertTrue(markdown.contains("## 6. Command Audit"));
        assertTrue(markdown.contains("## 7. Preflight Check"));
        assertTrue(markdown.contains("## 8. Git Diff Audit"));
        assertTrue(markdown.contains("## 9. Recommended Next Actions"));
        assertTrue(markdown.contains("**Risk Score:** 55"));
    }

    @Test
    void generate_withMinimalData_shouldHandleGracefully() {
        ProjectInfo projectInfo = new ProjectInfo();
        projectInfo.setId(1L);
        projectInfo.setProjectName("MinimalProject");
        projectInfo.setProjectPath("/tmp/minimal");
        projectInfo.setHasGit(false);
        projectInfo.setHasAgentsMd(false);

        MarkdownReportContext context = MarkdownReportContext.builder()
                .projectInfo(projectInfo)
                .latestScanResult(null)
                .agentRules(List.of())
                .latestRiskReports(List.of())
                .latestPermissionReport(null)
                .latestCommandAuditReport(null)
                .latestGitAuditReport(null)
                .latestPreflightReport(null)
                .build();

        String markdown = generator.generate(context, true, true, true, true, true);

        assertNotNull(markdown);
        assertTrue(markdown.contains("No scan result available"));
        assertTrue(markdown.contains("No agent rules generated yet"));
        assertTrue(markdown.contains("No permission assessment report available"));
        assertTrue(markdown.contains("No command audit report available"));
        assertTrue(markdown.contains("No preflight check report available"));
        assertTrue(markdown.contains("No git diff audit report available"));
        assertTrue(markdown.contains("Initialize Git"));
        assertTrue(markdown.contains("AGENTS.md"));
    }

    @Test
    void generate_withSelectiveInclusion_shouldRespectFlags() {
        MarkdownReportContext context = buildFullContext();

        String markdown = generator.generate(context, false, false, false, false, false);

        assertNotNull(markdown);
        assertTrue(markdown.contains("# TestProject Security Report"));
        assertTrue(markdown.contains("## 1. Project Overview"));
        assertFalse(markdown.contains("## 2. Latest Scan Summary"));
        assertFalse(markdown.contains("## 3. Agent Rule Status"));
        assertFalse(markdown.contains("## 4. Risk Report Summary"));
        assertFalse(markdown.contains("## 7. Preflight Check"));
        assertFalse(markdown.contains("## 8. Git Diff Audit"));
        assertTrue(markdown.contains("## 9. Recommended Next Actions"));
    }

    @Test
    void generate_shouldParseJsonListsCorrectly() {
        ScanResult scanResult = new ScanResult();
        scanResult.setFileCount(50);
        scanResult.setDirectoryCount(10);
        scanResult.setDetectedFiles("[\"pom.xml\", \"package.json\"]");
        scanResult.setSensitiveFiles("[\".env\"]");
        scanResult.setRiskLevel("MEDIUM");
        scanResult.setCreatedTime(LocalDateTime.now());

        ProjectInfo projectInfo = new ProjectInfo();
        projectInfo.setId(1L);
        projectInfo.setProjectName("TestProject");
        projectInfo.setProjectPath("/tmp/test");
        projectInfo.setHasGit(true);
        projectInfo.setHasAgentsMd(true);

        MarkdownReportContext context = MarkdownReportContext.builder()
                .projectInfo(projectInfo)
                .latestScanResult(scanResult)
                .agentRules(List.of())
                .latestRiskReports(List.of())
                .latestPermissionReport(null)
                .latestCommandAuditReport(null)
                .latestGitAuditReport(null)
                .latestPreflightReport(null)
                .build();

        String markdown = generator.generate(context, true, false, false, false, false);

        assertTrue(markdown.contains("pom.xml"));
        assertTrue(markdown.contains("package.json"));
        assertTrue(markdown.contains(".env"));
        assertTrue(markdown.contains("50"));
        assertTrue(markdown.contains("10"));
    }

    private MarkdownReportContext buildFullContext() {
        ProjectInfo projectInfo = new ProjectInfo();
        projectInfo.setId(1L);
        projectInfo.setProjectName("TestProject");
        projectInfo.setProjectPath("/tmp/test");
        projectInfo.setProjectType("FULL_STACK");
        projectInfo.setTechStack("[\"Java\", \"Vue\"]");
        projectInfo.setHasGit(true);
        projectInfo.setHasAgentsMd(true);

        ScanResult scanResult = new ScanResult();
        scanResult.setFileCount(100);
        scanResult.setDirectoryCount(20);
        scanResult.setDetectedFiles("[\"pom.xml\"]");
        scanResult.setSensitiveFiles("[\".env\"]");
        scanResult.setRiskLevel("MEDIUM");
        scanResult.setCreatedTime(LocalDateTime.now());

        AgentRule rule = new AgentRule();
        rule.setId(1L);
        rule.setAgentType("CODEX");
        rule.setFileName("AGENTS.md");
        rule.setCreatedTime(LocalDateTime.now());
        rule.setUpdatedTime(LocalDateTime.now());

        RiskReport permissionReport = new RiskReport();
        permissionReport.setId(1L);
        permissionReport.setReportType("PERMISSION_ASSESS");
        permissionReport.setRiskLevel("MEDIUM");
        permissionReport.setRiskScore(55);
        permissionReport.setSummary("权限风险评估完成，风险等级为 MEDIUM，风险分数为 55");
        permissionReport.setRiskItems("[\"允许修改工作区\"]");
        permissionReport.setSuggestions("[\"建议保持人工审批\"]");
        permissionReport.setCreatedTime(LocalDateTime.now());

        RiskReport commandReport = new RiskReport();
        commandReport.setId(2L);
        commandReport.setReportType("COMMAND_AUDIT");
        commandReport.setRiskLevel("LOW");
        commandReport.setRiskItems("[]");
        commandReport.setSuggestions("[]");
        commandReport.setCreatedTime(LocalDateTime.now());

        RiskReport preflightReport = new RiskReport();
        preflightReport.setId(3L);
        preflightReport.setReportType("PREFLIGHT_CHECK");
        preflightReport.setRiskLevel("LOW");
        preflightReport.setRiskItems("[]");
        preflightReport.setSuggestions("[]");
        preflightReport.setCreatedTime(LocalDateTime.now());

        RiskReport gitReport = new RiskReport();
        gitReport.setId(4L);
        gitReport.setReportType("GIT_DIFF_AUDIT");
        gitReport.setRiskLevel("LOW");
        gitReport.setRiskItems("[]");
        gitReport.setSuggestions("[]");
        gitReport.setCreatedTime(LocalDateTime.now());

        return MarkdownReportContext.builder()
                .projectInfo(projectInfo)
                .latestScanResult(scanResult)
                .agentRules(List.of(rule))
                .latestRiskReports(List.of(permissionReport, commandReport, preflightReport, gitReport))
                .latestPermissionReport(permissionReport)
                .latestCommandAuditReport(commandReport)
                .latestGitAuditReport(gitReport)
                .latestPreflightReport(preflightReport)
                .build();
    }
}
