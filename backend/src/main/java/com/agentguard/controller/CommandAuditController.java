package com.agentguard.controller;

import com.agentguard.common.PageResult;
import com.agentguard.common.Result;
import com.agentguard.dto.CommandAuditRequest;
import com.agentguard.service.CommandAuditService;
import com.agentguard.vo.CommandAuditVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/commands")
public class CommandAuditController {

    private final CommandAuditService commandAuditService;

    public CommandAuditController(CommandAuditService commandAuditService) {
        this.commandAuditService = commandAuditService;
    }

    @PostMapping("/audit")
    public Result<CommandAuditVO> auditCommands(@Valid @RequestBody CommandAuditRequest request) {
        return Result.success(commandAuditService.auditCommands(request));
    }

    @GetMapping("/reports/project/{projectId}")
    public Result<PageResult<CommandAuditVO>> pageProjectReports(@PathVariable Long projectId,
                                                                   @RequestParam(defaultValue = "1") long current,
                                                                   @RequestParam(defaultValue = "10") long size) {
        return Result.success(commandAuditService.pageProjectReports(projectId, current, size));
    }
}
