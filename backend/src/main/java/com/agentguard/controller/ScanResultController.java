package com.agentguard.controller;

import com.agentguard.common.PageResult;
import com.agentguard.common.Result;
import com.agentguard.service.ScanResultService;
import com.agentguard.vo.ScanResultDetailVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scan-results")
public class ScanResultController {

    private final ScanResultService scanResultService;

    public ScanResultController(ScanResultService scanResultService) {
        this.scanResultService = scanResultService;
    }

    @GetMapping("/project/{projectId}/latest")
    public Result<ScanResultDetailVO> getLatest(@PathVariable Long projectId) {
        return Result.success(scanResultService.getLatestByProjectId(projectId));
    }

    @GetMapping("/project/{projectId}")
    public Result<PageResult<ScanResultDetailVO>> pageByProject(@PathVariable Long projectId,
                                                                 @RequestParam(defaultValue = "1") long current,
                                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.success(scanResultService.pageByProjectId(projectId, current, size));
    }
}
