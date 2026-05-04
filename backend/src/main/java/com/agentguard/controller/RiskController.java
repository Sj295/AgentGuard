package com.agentguard.controller;

import com.agentguard.common.PageResult;
import com.agentguard.common.Result;
import com.agentguard.dto.PermissionAssessRequest;
import com.agentguard.service.RiskAssessService;
import com.agentguard.vo.PermissionAssessVO;
import com.agentguard.vo.RiskReportDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    private final RiskAssessService riskAssessService;

    public RiskController(RiskAssessService riskAssessService) {
        this.riskAssessService = riskAssessService;
    }

    @PostMapping("/permission-assess")
    public Result<PermissionAssessVO> assessPermission(@Valid @RequestBody PermissionAssessRequest request) {
        return Result.success(riskAssessService.assessPermission(request));
    }

    @GetMapping("/reports/project/{projectId}")
    public Result<List<PermissionAssessVO>> listProjectReports(@PathVariable Long projectId) {
        return Result.success(riskAssessService.listProjectReports(projectId));
    }

    @GetMapping("/reports/{reportId}")
    public Result<RiskReportDetailVO> getReportDetail(@PathVariable Long reportId) {
        return Result.success(riskAssessService.getReportDetail(reportId));
    }

    @GetMapping("/reports/project/{projectId}/type/{reportType}")
    public Result<PageResult<RiskReportDetailVO>> pageProjectReportsByType(@PathVariable Long projectId,
                                                                            @PathVariable String reportType,
                                                                            @RequestParam(defaultValue = "1") long current,
                                                                            @RequestParam(defaultValue = "10") long size) {
        return Result.success(riskAssessService.pageProjectReportsByType(projectId, reportType, current, size));
    }
}
