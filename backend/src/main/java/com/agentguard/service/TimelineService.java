package com.agentguard.service;

import com.agentguard.common.PageResult;
import com.agentguard.vo.ProjectSecurityOverviewVO;
import com.agentguard.vo.TimelineEventVO;

import java.util.List;

public interface TimelineService {

    PageResult<TimelineEventVO> getProjectTimeline(Long projectId, long current, long size, String riskLevel);

    ProjectSecurityOverviewVO getProjectOverview(Long projectId);

    List<TimelineEventVO> getHighRiskEvents(Long projectId, int limit);
}
