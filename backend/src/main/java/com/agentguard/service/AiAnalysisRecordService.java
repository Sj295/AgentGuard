package com.agentguard.service;

import com.agentguard.ai.vo.AiAnalysisRecordVO;
import com.agentguard.common.PageResult;
import com.agentguard.entity.AiAnalysisRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AiAnalysisRecordService extends IService<AiAnalysisRecord> {

    void saveRecordSafely(AiAnalysisRecord record);

    PageResult<AiAnalysisRecordVO> pageProjectRecords(Long projectId, String analysisType, long current, long size);

    AiAnalysisRecordVO getRecordDetail(Long id);

    List<AiAnalysisRecordVO> listLatestProjectRecords(Long projectId, int limit);
}
