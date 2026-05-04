package com.agentguard.controller;

import com.agentguard.common.PageResult;
import com.agentguard.common.Result;
import com.agentguard.dto.PreflightCheckRequest;
import com.agentguard.service.PreflightService;
import com.agentguard.vo.PreflightCheckVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preflight")
public class PreflightController {

    private final PreflightService preflightService;

    public PreflightController(PreflightService preflightService) {
        this.preflightService = preflightService;
    }

    @PostMapping("/check")
    public Result<PreflightCheckVO> check(@Valid @RequestBody PreflightCheckRequest request) {
        return Result.success(preflightService.check(request));
    }

    @GetMapping("/reports/project/{projectId}")
    public Result<PageResult<PreflightCheckVO>> pageProjectReports(@PathVariable Long projectId,
                                                                     @RequestParam(defaultValue = "1") long current,
                                                                     @RequestParam(defaultValue = "10") long size) {
        return Result.success(preflightService.pageProjectReports(projectId, current, size));
    }
}
