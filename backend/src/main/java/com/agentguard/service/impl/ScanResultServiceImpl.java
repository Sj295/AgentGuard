package com.agentguard.service.impl;

import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.JsonUtils;
import com.agentguard.common.PageResult;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.ScanResult;
import com.agentguard.mapper.ScanResultMapper;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.ScanResultService;
import com.agentguard.vo.ScanResultDetailVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ScanResultServiceImpl extends ServiceImpl<ScanResultMapper, ScanResult> implements ScanResultService {

    private final ProjectInfoService projectInfoService;

    public ScanResultServiceImpl(ProjectInfoService projectInfoService) {
        this.projectInfoService = projectInfoService;
    }

    @Override
    public ScanResultDetailVO getLatestByProjectId(Long projectId) {
        validateProjectExists(projectId);
        ScanResult scanResult = lambdaQuery()
                .eq(ScanResult::getProjectId, projectId)
                .orderByDesc(ScanResult::getCreatedTime)
                .orderByDesc(ScanResult::getId)
                .last("limit 1")
                .one();
        if (scanResult == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND, "Scan result not found");
        }
        return toScanResultDetailVO(scanResult);
    }

    @Override
    public PageResult<ScanResultDetailVO> pageByProjectId(Long projectId, long current, long size) {
        validateProjectExists(projectId);
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Pagination parameters must be greater than 0");
        }
        Page<ScanResult> page = this.page(
                new Page<>(current, size),
                Wrappers.<ScanResult>lambdaQuery()
                        .eq(ScanResult::getProjectId, projectId)
                        .orderByDesc(ScanResult::getCreatedTime)
                        .orderByDesc(ScanResult::getId)
        );
        Page<ScanResultDetailVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toScanResultDetailVO).toList());
        return PageResult.fromPage(voPage);
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

    private ScanResultDetailVO toScanResultDetailVO(ScanResult scanResult) {
        ScanResultDetailVO vo = new ScanResultDetailVO();
        vo.setId(scanResult.getId());
        vo.setProjectId(scanResult.getProjectId());
        vo.setTaskId(scanResult.getTaskId());
        vo.setFileCount(scanResult.getFileCount());
        vo.setDirectoryCount(scanResult.getDirectoryCount());
        vo.setDetectedFiles(JsonUtils.parseStringList(scanResult.getDetectedFiles()));
        vo.setDetectedCommands(JsonUtils.parseStringList(scanResult.getDetectedCommands()));
        vo.setSensitiveFiles(JsonUtils.parseStringList(scanResult.getSensitiveFiles()));
        vo.setRiskLevel(scanResult.getRiskLevel());
        vo.setCreatedTime(scanResult.getCreatedTime());
        return vo;
    }
}
