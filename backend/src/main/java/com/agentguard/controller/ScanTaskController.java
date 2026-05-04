package com.agentguard.controller;

import com.agentguard.common.PageResult;
import com.agentguard.common.Result;
import com.agentguard.service.ScanTaskService;
import com.agentguard.vo.ScanResultVO;
import com.agentguard.vo.ScanTaskVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scan-tasks")
public class ScanTaskController {

    private final ScanTaskService scanTaskService;

    public ScanTaskController(ScanTaskService scanTaskService) {
        this.scanTaskService = scanTaskService;
    }

    @GetMapping("/project/{projectId}")
    public Result<PageResult<ScanTaskVO>> pageProjectTasks(@PathVariable Long projectId,
                                                           @RequestParam(defaultValue = "1") long current,
                                                           @RequestParam(defaultValue = "10") long size) {
        return Result.success(scanTaskService.pageByProjectId(projectId, current, size));
    }

    @GetMapping("/{taskId}")
    public Result<ScanTaskVO> getTaskDetail(@PathVariable Long taskId) {
        return Result.success(scanTaskService.getTaskDetail(taskId));
    }

    @GetMapping("/{taskId}/result")
    public Result<ScanResultVO> getTaskResult(@PathVariable Long taskId) {
        return Result.success(scanTaskService.getTaskResult(taskId));
    }
}
