package com.agentguard.service.impl;

import com.agentguard.common.enums.TaskStatus;
import com.agentguard.detector.ProjectRiskDetector;
import com.agentguard.detector.SensitiveFileDetector;
import com.agentguard.detector.TechStackDetector;
import com.agentguard.dto.ProjectScanRequest;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.ScanResult;
import com.agentguard.entity.ScanTask;
import com.agentguard.scanner.ProjectScanner;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.ProjectScanService;
import com.agentguard.service.ScanResultService;
import com.agentguard.service.ScanTaskService;
import com.agentguard.vo.ProjectScanVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectScanServiceImpl implements ProjectScanService {

    private static final String TASK_TYPE_PROJECT_SCAN = "PROJECT_SCAN";

    private final ProjectScanner projectScanner;
    private final TechStackDetector techStackDetector;
    private final SensitiveFileDetector sensitiveFileDetector;
    private final ProjectRiskDetector projectRiskDetector;
    private final ProjectInfoService projectInfoService;
    private final ScanTaskService scanTaskService;
    private final ScanResultService scanResultService;
    private final ObjectMapper objectMapper;

    public ProjectScanServiceImpl(ProjectScanner projectScanner,
                                  TechStackDetector techStackDetector,
                                  SensitiveFileDetector sensitiveFileDetector,
                                  ProjectRiskDetector projectRiskDetector,
                                  ProjectInfoService projectInfoService,
                                  ScanTaskService scanTaskService,
                                  ScanResultService scanResultService,
                                  ObjectMapper objectMapper) {
        this.projectScanner = projectScanner;
        this.techStackDetector = techStackDetector;
        this.sensitiveFileDetector = sensitiveFileDetector;
        this.projectRiskDetector = projectRiskDetector;
        this.projectInfoService = projectInfoService;
        this.scanTaskService = scanTaskService;
        this.scanResultService = scanResultService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectScanVO scanProject(ProjectScanRequest request) {
        ProjectScanner.ProjectScanContext scanContext = projectScanner.scan(request.getProjectPath());
        TechStackDetector.TechStackDetectResult techStackResult = techStackDetector.detect(scanContext);
        List<String> sensitiveFiles = sensitiveFileDetector.detectSensitiveFiles(scanContext.getAllFilePaths());
        ProjectRiskDetector.ProjectRiskAssessment riskAssessment = projectRiskDetector.assess(scanContext, sensitiveFiles);

        ProjectInfo projectInfo = saveOrUpdateProjectInfo(request, scanContext, techStackResult);
        ScanTask runningTask = createRunningTask(projectInfo.getId());
        saveScanResult(projectInfo.getId(), runningTask.getId(), scanContext, sensitiveFiles, riskAssessment);
        markTaskSuccess(runningTask.getId(), projectInfo.getId(), scanContext, sensitiveFiles);
        return buildScanVO(projectInfo, runningTask.getId(), techStackResult, scanContext, sensitiveFiles, riskAssessment);
    }

    private ScanTask createRunningTask(Long projectId) {
        ScanTask scanTask = new ScanTask();
        scanTask.setProjectId(projectId);
        scanTask.setTaskType(TASK_TYPE_PROJECT_SCAN);
        scanTask.setStatus(TaskStatus.RUNNING.name());
        scanTask.setProgress(0);
        LocalDateTime now = LocalDateTime.now();
        scanTask.setCreatedTime(now);
        scanTask.setUpdatedTime(now);
        scanTaskService.save(scanTask);
        return scanTask;
    }

    private ProjectInfo saveOrUpdateProjectInfo(ProjectScanRequest request,
                                                ProjectScanner.ProjectScanContext scanContext,
                                                TechStackDetector.TechStackDetectResult techStackResult) {
        String normalizedProjectPath = scanContext.getNormalizedProjectPath();
        ProjectInfo projectInfo = projectInfoService.lambdaQuery()
                .eq(ProjectInfo::getProjectPath, normalizedProjectPath)
                .last("limit 1")
                .one();
        if (projectInfo == null) {
            projectInfo = new ProjectInfo();
        }
        projectInfo.setProjectName(request.getProjectName());
        projectInfo.setProjectPath(normalizedProjectPath);
        projectInfo.setProjectType(techStackResult.getProjectType());
        projectInfo.setTechStack(toJson(techStackResult.getTechStack()));
        projectInfo.setHasGit(scanContext.isHasGit());
        projectInfo.setHasAgentsMd(scanContext.isHasAgentsMd());
        if (projectInfo.getId() == null) {
            projectInfoService.save(projectInfo);
        } else {
            projectInfoService.updateById(projectInfo);
        }
        return projectInfo;
    }

    private void saveScanResult(Long projectId,
                                Long taskId,
                                ProjectScanner.ProjectScanContext scanContext,
                                List<String> sensitiveFiles,
                                ProjectRiskDetector.ProjectRiskAssessment riskAssessment) {
        ScanResult scanResult = new ScanResult();
        scanResult.setProjectId(projectId);
        scanResult.setTaskId(taskId);
        scanResult.setFileCount(safeToInt(scanContext.getFileCount()));
        scanResult.setDirectoryCount(safeToInt(scanContext.getDirectoryCount()));
        scanResult.setDetectedFiles(toJson(scanContext.getDetectedFiles()));
        scanResult.setDetectedCommands(toJson(List.of()));
        scanResult.setSensitiveFiles(toJson(sensitiveFiles));
        scanResult.setRiskLevel(riskAssessment.getRiskLevel());
        scanResultService.save(scanResult);
    }

    private void markTaskSuccess(Long taskId,
                                 Long projectId,
                                 ProjectScanner.ProjectScanContext scanContext,
                                 List<String> sensitiveFiles) {
        ScanTask taskUpdate = new ScanTask();
        taskUpdate.setId(taskId);
        taskUpdate.setProjectId(projectId);
        taskUpdate.setStatus(TaskStatus.SUCCESS.name());
        taskUpdate.setProgress(100);
        taskUpdate.setResultSummary(String.format(
                "Scan completed: files=%d, directories=%d, sensitiveFiles=%d",
                scanContext.getFileCount(),
                scanContext.getDirectoryCount(),
                sensitiveFiles.size()
        ));
        taskUpdate.setErrorMessage(null);
        taskUpdate.setUpdatedTime(LocalDateTime.now());
        scanTaskService.updateById(taskUpdate);
    }

    private ProjectScanVO buildScanVO(ProjectInfo projectInfo,
                                      Long taskId,
                                      TechStackDetector.TechStackDetectResult techStackResult,
                                      ProjectScanner.ProjectScanContext scanContext,
                                      List<String> sensitiveFiles,
                                      ProjectRiskDetector.ProjectRiskAssessment riskAssessment) {
        ProjectScanVO projectScanVO = new ProjectScanVO();
        projectScanVO.setProjectId(projectInfo.getId());
        projectScanVO.setTaskId(taskId);
        projectScanVO.setProjectName(projectInfo.getProjectName());
        projectScanVO.setProjectPath(projectInfo.getProjectPath());
        projectScanVO.setProjectType(techStackResult.getProjectType());
        projectScanVO.setTechStack(techStackResult.getTechStack());
        projectScanVO.setFileCount(scanContext.getFileCount());
        projectScanVO.setDirectoryCount(scanContext.getDirectoryCount());
        projectScanVO.setHasGit(scanContext.isHasGit());
        projectScanVO.setHasAgentsMd(scanContext.isHasAgentsMd());
        projectScanVO.setDetectedFiles(scanContext.getDetectedFiles());
        projectScanVO.setSensitiveFiles(sensitiveFiles);
        projectScanVO.setRiskLevel(riskAssessment.getRiskLevel());
        projectScanVO.setSuggestions(riskAssessment.getSuggestions());
        return projectScanVO;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to serialize object to JSON", exception);
        }
    }

    private Integer safeToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }
}
