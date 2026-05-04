package com.agentguard.service.impl;

import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.JsonUtils;
import com.agentguard.common.enums.AgentType;
import com.agentguard.dto.AgentRuleGenerateRequest;
import com.agentguard.entity.AgentRule;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.ScanResult;
import com.agentguard.generator.AgentRuleGenerateContext;
import com.agentguard.generator.AgentRuleGenerator;
import com.agentguard.generator.AgentRuleGeneratorFactory;
import com.agentguard.service.AgentRuleGenerateService;
import com.agentguard.service.AgentRuleService;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.ScanResultService;
import com.agentguard.vo.AgentRuleGenerateVO;
import com.agentguard.vo.AgentRuleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AgentRuleGenerateServiceImpl implements AgentRuleGenerateService {

    private final ProjectInfoService projectInfoService;
    private final ScanResultService scanResultService;
    private final AgentRuleService agentRuleService;
    private final AgentRuleGeneratorFactory generatorFactory;
    public AgentRuleGenerateServiceImpl(ProjectInfoService projectInfoService,
                                        ScanResultService scanResultService,
                                        AgentRuleService agentRuleService,
                                        AgentRuleGeneratorFactory generatorFactory) {
        this.projectInfoService = projectInfoService;
        this.scanResultService = scanResultService;
        this.agentRuleService = agentRuleService;
        this.generatorFactory = generatorFactory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentRuleGenerateVO generateRule(AgentRuleGenerateRequest request) {
        AgentType agentType = AgentType.fromCode(request.getAgentType());
        ProjectInfo projectInfo = loadProjectInfo(request.getProjectId());
        ScanResult latestScanResult = loadLatestScanResult(request.getProjectId());
        List<String> techStack = JsonUtils.parseStringList(projectInfo.getTechStack());
        List<String> detectedFiles = JsonUtils.parseStringList(latestScanResult.getDetectedFiles());
        List<String> sensitiveFiles = JsonUtils.parseStringList(latestScanResult.getSensitiveFiles());
        List<String> detectedCommands = JsonUtils.parseStringList(latestScanResult.getDetectedCommands());
        List<String> suggestions = buildSuggestions(projectInfo, detectedFiles, sensitiveFiles);

        AgentRuleGenerateContext context = AgentRuleGenerateContext.builder()
                .projectInfo(projectInfo)
                .latestScanResult(latestScanResult)
                .techStack(techStack)
                .detectedFiles(detectedFiles)
                .sensitiveFiles(sensitiveFiles)
                .detectedCommands(detectedCommands)
                .riskLevel(defaultRiskLevel(latestScanResult.getRiskLevel()))
                .suggestions(suggestions)
                .build();

        AgentRuleGenerator generator = generatorFactory.getGenerator(agentType.getCode());
        String content = generator.generate(context);
        AgentRule savedRule = saveOrUpdateRule(projectInfo.getId(), agentType, generator.getFileName(), content);
        return toGenerateVO(savedRule, projectInfo.getProjectPath(), agentType);
    }

    @Override
    public List<AgentRuleGenerateVO> listByProjectId(Long projectId) {
        ProjectInfo projectInfo = loadProjectInfo(projectId);
        List<AgentRule> rules = agentRuleService.lambdaQuery()
                .eq(AgentRule::getProjectId, projectId)
                .orderByDesc(AgentRule::getUpdatedTime)
                .orderByDesc(AgentRule::getCreatedTime)
                .list();
        List<AgentRuleGenerateVO> result = new ArrayList<>();
        for (AgentRule rule : rules) {
            AgentType agentType = AgentType.fromCode(rule.getAgentType());
            result.add(toGenerateVO(rule, projectInfo.getProjectPath(), agentType));
        }
        return result;
    }

    @Override
    public AgentRuleVO getRuleDetail(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Rule id cannot be null");
        }
        AgentRule agentRule = agentRuleService.getById(id);
        if (agentRule == null) {
            throw new BusinessException(ErrorCode.RULE_NOT_FOUND);
        }
        return toAgentRuleVO(agentRule);
    }

    @Override
    public AgentRuleVO getLatestByProjectAndAgentType(Long projectId, String agentType) {
        loadProjectInfo(projectId);
        AgentType parsedAgentType = AgentType.fromCode(agentType);
        AgentRule agentRule = agentRuleService.lambdaQuery()
                .eq(AgentRule::getProjectId, projectId)
                .eq(AgentRule::getAgentType, parsedAgentType.getCode())
                .orderByDesc(AgentRule::getUpdatedTime)
                .orderByDesc(AgentRule::getCreatedTime)
                .orderByDesc(AgentRule::getId)
                .last("limit 1")
                .one();
        if (agentRule == null) {
            throw new BusinessException(ErrorCode.RULE_NOT_FOUND);
        }
        return toAgentRuleVO(agentRule);
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

    private ScanResult loadLatestScanResult(Long projectId) {
        ScanResult latestScanResult = scanResultService.lambdaQuery()
                .eq(ScanResult::getProjectId, projectId)
                .orderByDesc(ScanResult::getCreatedTime)
                .orderByDesc(ScanResult::getId)
                .last("limit 1")
                .one();
        if (latestScanResult == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Please scan the project first.");
        }
        return latestScanResult;
    }

    private AgentRule saveOrUpdateRule(Long projectId, AgentType agentType, String fileName, String content) {
        AgentRule existingRule = agentRuleService.lambdaQuery()
                .eq(AgentRule::getProjectId, projectId)
                .eq(AgentRule::getAgentType, agentType.getCode())
                .last("limit 1")
                .one();
        LocalDateTime now = LocalDateTime.now();
        if (existingRule == null) {
            AgentRule newRule = new AgentRule();
            newRule.setProjectId(projectId);
            newRule.setAgentType(agentType.getCode());
            newRule.setFileName(fileName);
            newRule.setContent(content);
            newRule.setCreatedTime(now);
            newRule.setUpdatedTime(now);
            agentRuleService.save(newRule);
            return newRule;
        }
        existingRule.setFileName(fileName);
        existingRule.setContent(content);
        existingRule.setUpdatedTime(now);
        agentRuleService.updateById(existingRule);
        return existingRule;
    }

    private AgentRuleGenerateVO toGenerateVO(AgentRule rule, String projectPath, AgentType agentType) {
        AgentRuleGenerateVO vo = new AgentRuleGenerateVO();
        vo.setId(rule.getId());
        vo.setProjectId(rule.getProjectId());
        vo.setAgentType(rule.getAgentType());
        vo.setFileName(rule.getFileName());
        vo.setSuggestedPath(agentType.resolveSuggestedPath(projectPath));
        vo.setContent(rule.getContent());
        vo.setCreatedTime(rule.getCreatedTime());
        return vo;
    }

    private AgentRuleVO toAgentRuleVO(AgentRule rule) {
        AgentRuleVO vo = new AgentRuleVO();
        vo.setId(rule.getId());
        vo.setProjectId(rule.getProjectId());
        vo.setAgentType(rule.getAgentType());
        vo.setFileName(rule.getFileName());
        vo.setContent(rule.getContent());
        vo.setCreatedTime(rule.getCreatedTime());
        vo.setUpdatedTime(rule.getUpdatedTime());
        return vo;
    }

    private String defaultRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return "LOW";
        }
        return riskLevel.toUpperCase(Locale.ROOT);
    }

    private List<String> buildSuggestions(ProjectInfo projectInfo, List<String> detectedFiles, List<String> sensitiveFiles) {
        Set<String> normalizedDetected = new LinkedHashSet<>();
        if (detectedFiles != null) {
            for (String detectedFile : detectedFiles) {
                if (detectedFile != null) {
                    normalizedDetected.add(detectedFile.toLowerCase(Locale.ROOT));
                }
            }
        }
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        if (!Boolean.TRUE.equals(projectInfo.getHasAgentsMd())) {
            suggestions.add("建议生成 AGENTS.md 以提升 AI Coding Agent 的项目理解能力。");
        }
        if (sensitiveFiles != null && !sensitiveFiles.isEmpty()) {
            suggestions.add("检测到敏感配置文件，建议在 Agent 规则中禁止读取或修改。");
        }
        if (!Boolean.TRUE.equals(projectInfo.getHasGit())) {
            suggestions.add("当前项目未检测到 Git 仓库，建议先初始化 Git 或创建备份后再让 AI Agent 修改代码。");
        }
        if (normalizedDetected.contains("package.json")) {
            suggestions.add("检测到前端依赖文件，AI Agent 修改后建议执行 npm install 或 npm run build。");
        }
        if (normalizedDetected.contains("pom.xml")) {
            suggestions.add("检测到 Maven 项目，AI Agent 修改后建议执行 mvn test 或 mvn package。");
        }
        return new ArrayList<>(suggestions);
    }
}
