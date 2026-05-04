package com.agentguard.controller;

import com.agentguard.common.Result;
import com.agentguard.dto.GitDiffAuditRequest;
import com.agentguard.service.GitAuditService;
import com.agentguard.vo.GitAuditReportDetailVO;
import com.agentguard.vo.GitDiffAuditVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/git-audit")
public class GitAuditController {

    private final GitAuditService gitAuditService;

    public GitAuditController(GitAuditService gitAuditService) {
        this.gitAuditService = gitAuditService;
    }

    @PostMapping("/diff")
    public Result<GitDiffAuditVO> auditDiff(@Valid @RequestBody GitDiffAuditRequest request) {
        return Result.success(gitAuditService.auditDiff(request));
    }

    @GetMapping("/reports/project/{projectId}")
    public Result<List<GitDiffAuditVO>> listProjectReports(@PathVariable Long projectId) {
        return Result.success(gitAuditService.listProjectReports(projectId));
    }

    @GetMapping("/reports/{reportId}")
    public Result<GitAuditReportDetailVO> getReportDetail(@PathVariable Long reportId) {
        return Result.success(gitAuditService.getReportDetail(reportId));
    }
}
