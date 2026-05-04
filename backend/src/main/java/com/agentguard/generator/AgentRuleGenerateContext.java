package com.agentguard.generator;

import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.ScanResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRuleGenerateContext {

    private ProjectInfo projectInfo;

    private ScanResult latestScanResult;

    private List<String> techStack;

    private List<String> detectedFiles;

    private List<String> sensitiveFiles;

    private List<String> detectedCommands;

    private String riskLevel;

    private List<String> suggestions;
}
