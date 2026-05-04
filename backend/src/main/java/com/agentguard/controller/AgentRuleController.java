package com.agentguard.controller;

import com.agentguard.common.Result;
import com.agentguard.dto.AgentRuleGenerateRequest;
import com.agentguard.dto.AgentRuleWriteRequest;
import com.agentguard.service.AgentRuleFileService;
import com.agentguard.service.AgentRuleGenerateService;
import com.agentguard.vo.AgentRuleGenerateVO;
import com.agentguard.vo.AgentRuleVO;
import com.agentguard.vo.AgentRuleWriteVO;
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
@RequestMapping("/api/agent-rules")
public class AgentRuleController {

    private final AgentRuleGenerateService agentRuleGenerateService;
    private final AgentRuleFileService agentRuleFileService;

    public AgentRuleController(AgentRuleGenerateService agentRuleGenerateService,
                               AgentRuleFileService agentRuleFileService) {
        this.agentRuleGenerateService = agentRuleGenerateService;
        this.agentRuleFileService = agentRuleFileService;
    }

    @PostMapping("/generate")
    public Result<AgentRuleGenerateVO> generateRule(@Valid @RequestBody AgentRuleGenerateRequest request) {
        return Result.success(agentRuleGenerateService.generateRule(request));
    }

    @GetMapping("/project/{projectId}")
    public Result<List<AgentRuleGenerateVO>> listProjectRules(@PathVariable Long projectId) {
        return Result.success(agentRuleGenerateService.listByProjectId(projectId));
    }

    @GetMapping("/{id}")
    public Result<AgentRuleVO> getRuleDetail(@PathVariable Long id) {
        return Result.success(agentRuleGenerateService.getRuleDetail(id));
    }

    @GetMapping("/project/{projectId}/latest")
    public Result<AgentRuleVO> getLatestByProjectAndAgentType(@PathVariable Long projectId,
                                                              @RequestParam String agentType) {
        return Result.success(agentRuleGenerateService.getLatestByProjectAndAgentType(projectId, agentType));
    }

    @PostMapping("/{id}/write")
    public Result<AgentRuleWriteVO> writeRuleById(@PathVariable Long id,
                                                   @RequestBody AgentRuleWriteRequest request) {
        return Result.success(agentRuleFileService.writeRuleById(id, request));
    }

    @PostMapping("/project/{projectId}/write")
    public Result<AgentRuleWriteVO> writeLatestRule(@PathVariable Long projectId,
                                                     @RequestBody AgentRuleWriteRequest request) {
        return Result.success(agentRuleFileService.writeLatestRule(projectId, request));
    }
}
