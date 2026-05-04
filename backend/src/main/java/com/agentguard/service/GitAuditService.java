package com.agentguard.service;

import com.agentguard.dto.GitDiffAuditRequest;
import com.agentguard.vo.GitAuditReportDetailVO;
import com.agentguard.vo.GitDiffAuditVO;

import java.util.List;

public interface GitAuditService {

    GitDiffAuditVO auditDiff(GitDiffAuditRequest request);

    List<GitDiffAuditVO> listProjectReports(Long projectId);

    GitAuditReportDetailVO getReportDetail(Long reportId);
}
