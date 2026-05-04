package com.agentguard.service.impl;

import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.enums.AgentType;
import com.agentguard.dto.AgentRuleWriteRequest;
import com.agentguard.entity.AgentRule;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.generator.AgentRuleFileWriter;
import com.agentguard.service.AgentRuleFileService;
import com.agentguard.service.AgentRuleService;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.vo.AgentRuleWriteVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentRuleFileServiceImpl implements AgentRuleFileService {

    private final AgentRuleService agentRuleService;
    private final ProjectInfoService projectInfoService;
    private final AgentRuleFileWriter agentRuleFileWriter;

    public AgentRuleFileServiceImpl(AgentRuleService agentRuleService,
                                    ProjectInfoService projectInfoService,
                                    AgentRuleFileWriter agentRuleFileWriter) {
        this.agentRuleService = agentRuleService;
        this.projectInfoService = projectInfoService;
        this.agentRuleFileWriter = agentRuleFileWriter;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentRuleWriteVO writeRuleById(Long ruleId, AgentRuleWriteRequest request) {
        if (ruleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Rule id cannot be null");
        }
        AgentRule rule = agentRuleService.getById(ruleId);
        if (rule == null) {
            throw new BusinessException(ErrorCode.RULE_NOT_FOUND);
        }
        ProjectInfo project = loadProjectInfo(rule.getProjectId());
        boolean overwrite = Boolean.TRUE.equals(request.getOverwrite());
        boolean backup = !Boolean.FALSE.equals(request.getBackup());

        AgentRuleWriteVO vo = agentRuleFileWriter.write(rule, project, overwrite, backup);
        updateHasAgentsMdIfNeeded(project, rule.getFileName());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentRuleWriteVO writeLatestRule(Long projectId, AgentRuleWriteRequest request) {
        if (request.getAgentType() == null || request.getAgentType().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "agentType cannot be blank");
        }
        AgentType agentType = AgentType.fromCode(request.getAgentType());
        ProjectInfo project = loadProjectInfo(projectId);

        AgentRule rule = agentRuleService.lambdaQuery()
                .eq(AgentRule::getProjectId, projectId)
                .eq(AgentRule::getAgentType, agentType.getCode())
                .orderByDesc(AgentRule::getUpdatedTime)
                .orderByDesc(AgentRule::getCreatedTime)
                .orderByDesc(AgentRule::getId)
                .last("limit 1")
                .one();
        if (rule == null) {
            throw new BusinessException(ErrorCode.RULE_NOT_FOUND, "Please generate the rule first.");
        }

        boolean overwrite = Boolean.TRUE.equals(request.getOverwrite());
        boolean backup = !Boolean.FALSE.equals(request.getBackup());

        AgentRuleWriteVO vo = agentRuleFileWriter.write(rule, project, overwrite, backup);
        updateHasAgentsMdIfNeeded(project, rule.getFileName());
        return vo;
    }

    private ProjectInfo loadProjectInfo(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Project id cannot be null");
        }
        ProjectInfo projectInfo = projectInfoService.getById(projectId);
        if (projectInfo == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return projectInfo;
    }

    private void updateHasAgentsMdIfNeeded(ProjectInfo project, String fileName) {
        if ("AGENTS.md".equals(fileName) && !Boolean.TRUE.equals(project.getHasAgentsMd())) {
            project.setHasAgentsMd(true);
            projectInfoService.updateById(project);
        }
    }
}
