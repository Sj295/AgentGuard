package com.agentguard.service.impl;

import com.agentguard.ai.vo.AiAnalysisRecordVO;
import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.PageResult;
import com.agentguard.common.enums.AiAnalysisType;
import com.agentguard.entity.AiAnalysisRecord;
import com.agentguard.mapper.AiAnalysisRecordMapper;
import com.agentguard.service.AiAnalysisRecordService;
import com.agentguard.service.ProjectInfoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiAnalysisRecordServiceImpl extends ServiceImpl<AiAnalysisRecordMapper, AiAnalysisRecord>
        implements AiAnalysisRecordService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisRecordServiceImpl.class);

    private final ProjectInfoService projectInfoService;

    public AiAnalysisRecordServiceImpl(ProjectInfoService projectInfoService) {
        this.projectInfoService = projectInfoService;
    }

    @Override
    public void saveRecordSafely(AiAnalysisRecord record) {
        if (record == null) {
            return;
        }
        try {
            save(record);
        } catch (Exception exception) {
            log.warn("Failed to persist ai analysis record: {}", exception.getMessage());
        }
    }

    @Override
    public PageResult<AiAnalysisRecordVO> pageProjectRecords(Long projectId, String analysisType, long current, long size) {
        validateProject(projectId);
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Pagination parameters must be greater than 0");
        }
        String normalizedType = normalizeAnalysisType(analysisType);
        Page<AiAnalysisRecord> page = page(
                new Page<>(current, size),
                Wrappers.<AiAnalysisRecord>lambdaQuery()
                        .eq(AiAnalysisRecord::getProjectId, projectId)
                        .eq(StringUtils.hasText(normalizedType), AiAnalysisRecord::getAnalysisType, normalizedType)
                        .orderByDesc(AiAnalysisRecord::getCreatedTime)
                        .orderByDesc(AiAnalysisRecord::getId)
        );
        Page<AiAnalysisRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return PageResult.fromPage(voPage);
    }

    @Override
    public AiAnalysisRecordVO getRecordDetail(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Record id cannot be null");
        }
        AiAnalysisRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI analysis record does not exist");
        }
        return toVO(record);
    }

    @Override
    public List<AiAnalysisRecordVO> listLatestProjectRecords(Long projectId, int limit) {
        validateProject(projectId);
        int normalizedLimit = Math.max(1, Math.min(limit <= 0 ? 5 : limit, 50));
        List<AiAnalysisRecord> records = lambdaQuery()
                .eq(AiAnalysisRecord::getProjectId, projectId)
                .orderByDesc(AiAnalysisRecord::getCreatedTime)
                .orderByDesc(AiAnalysisRecord::getId)
                .last("limit " + normalizedLimit)
                .list();
        return records.stream().map(this::toVO).toList();
    }

    private void validateProject(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Project id cannot be null");
        }
        if (projectInfoService.getById(projectId) == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
    }

    private String normalizeAnalysisType(String analysisType) {
        if (!StringUtils.hasText(analysisType)) {
            return null;
        }
        return AiAnalysisType.fromCode(analysisType).name();
    }

    private AiAnalysisRecordVO toVO(AiAnalysisRecord record) {
        AiAnalysisRecordVO vo = new AiAnalysisRecordVO();
        vo.setId(record.getId());
        vo.setProjectId(record.getProjectId());
        vo.setAnalysisType(record.getAnalysisType());
        vo.setSourceReportId(record.getSourceReportId());
        vo.setProvider(record.getProvider());
        vo.setModel(record.getModel());
        vo.setMocked(record.getMocked());
        vo.setSuccess(record.getSuccess());
        vo.setLatencyMs(record.getLatencyMs());
        vo.setInputSummary(record.getInputSummary());
        vo.setOutputContent(record.getOutputContent());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setCreatedTime(record.getCreatedTime());
        return vo;
    }
}
