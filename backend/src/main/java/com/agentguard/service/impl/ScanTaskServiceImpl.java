package com.agentguard.service.impl;

import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.PageResult;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.ScanResult;
import com.agentguard.entity.ScanTask;
import com.agentguard.mapper.ScanTaskMapper;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.ScanResultService;
import com.agentguard.service.ScanTaskService;
import com.agentguard.vo.ScanResultVO;
import com.agentguard.vo.ScanTaskVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ScanTaskServiceImpl extends ServiceImpl<ScanTaskMapper, ScanTask> implements ScanTaskService {

    private final ProjectInfoService projectInfoService;
    private final ScanResultService scanResultService;

    public ScanTaskServiceImpl(ProjectInfoService projectInfoService, ScanResultService scanResultService) {
        this.projectInfoService = projectInfoService;
        this.scanResultService = scanResultService;
    }

    @Override
    public PageResult<ScanTaskVO> pageByProjectId(Long projectId, long current, long size) {
        validateProjectExists(projectId);
        validatePage(current, size);
        Page<ScanTask> page = this.page(
                new Page<>(current, size),
                Wrappers.<ScanTask>lambdaQuery()
                        .eq(ScanTask::getProjectId, projectId)
                        .orderByDesc(ScanTask::getCreatedTime)
                        .orderByDesc(ScanTask::getId)
        );
        Page<ScanTaskVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toScanTaskVO).toList());
        return PageResult.fromPage(voPage);
    }

    @Override
    public ScanTaskVO getTaskDetail(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Task id cannot be null");
        }
        ScanTask scanTask = this.getById(taskId);
        if (scanTask == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        return toScanTaskVO(scanTask);
    }

    @Override
    public ScanResultVO getTaskResult(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Task id cannot be null");
        }
        ScanTask scanTask = this.getById(taskId);
        if (scanTask == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        ScanResult scanResult = scanResultService.lambdaQuery()
                .eq(ScanResult::getTaskId, taskId)
                .orderByDesc(ScanResult::getCreatedTime)
                .orderByDesc(ScanResult::getId)
                .last("limit 1")
                .one();
        if (scanResult == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND, "Scan result not found");
        }
        return toScanResultVO(scanResult);
    }

    private void validateProjectExists(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Project id cannot be null");
        }
        ProjectInfo projectInfo = projectInfoService.getById(projectId);
        if (projectInfo == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
    }

    private void validatePage(long current, long size) {
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Pagination parameters must be greater than 0");
        }
    }

    private ScanTaskVO toScanTaskVO(ScanTask scanTask) {
        ScanTaskVO vo = new ScanTaskVO();
        vo.setId(scanTask.getId());
        vo.setProjectId(scanTask.getProjectId());
        vo.setTaskType(scanTask.getTaskType());
        vo.setStatus(scanTask.getStatus());
        vo.setProgress(scanTask.getProgress());
        vo.setResultSummary(scanTask.getResultSummary());
        vo.setErrorMessage(scanTask.getErrorMessage());
        vo.setCreatedTime(scanTask.getCreatedTime());
        vo.setUpdatedTime(scanTask.getUpdatedTime());
        return vo;
    }

    private ScanResultVO toScanResultVO(ScanResult scanResult) {
        ScanResultVO vo = new ScanResultVO();
        vo.setId(scanResult.getId());
        vo.setProjectId(scanResult.getProjectId());
        vo.setTaskId(scanResult.getTaskId());
        vo.setFileCount(scanResult.getFileCount());
        vo.setDirectoryCount(scanResult.getDirectoryCount());
        vo.setDetectedFiles(scanResult.getDetectedFiles());
        vo.setDetectedCommands(scanResult.getDetectedCommands());
        vo.setSensitiveFiles(scanResult.getSensitiveFiles());
        vo.setRiskLevel(scanResult.getRiskLevel());
        vo.setCreatedTime(scanResult.getCreatedTime());
        return vo;
    }
}
