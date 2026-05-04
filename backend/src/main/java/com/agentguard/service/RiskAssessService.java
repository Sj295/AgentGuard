package com.agentguard.service;

import com.agentguard.common.PageResult;
import com.agentguard.dto.PermissionAssessRequest;
import com.agentguard.vo.PermissionAssessVO;
import com.agentguard.vo.RiskReportDetailVO;

import java.util.List;

public interface RiskAssessService {

    PermissionAssessVO assessPermission(PermissionAssessRequest request);

    List<PermissionAssessVO> listProjectReports(Long projectId);

    RiskReportDetailVO getReportDetail(Long reportId);

    PageResult<RiskReportDetailVO> pageProjectReportsByType(Long projectId, String reportType, long current, long size);
}
