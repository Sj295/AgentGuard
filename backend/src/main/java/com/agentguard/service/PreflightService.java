package com.agentguard.service;

import com.agentguard.common.PageResult;
import com.agentguard.dto.PreflightCheckRequest;
import com.agentguard.vo.PreflightCheckVO;

public interface PreflightService {

    PreflightCheckVO check(PreflightCheckRequest request);

    PageResult<PreflightCheckVO> pageProjectReports(Long projectId, long current, long size);
}
