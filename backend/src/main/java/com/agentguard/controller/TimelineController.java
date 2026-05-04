package com.agentguard.controller;

import com.agentguard.common.PageResult;
import com.agentguard.common.Result;
import com.agentguard.service.TimelineService;
import com.agentguard.vo.ProjectSecurityOverviewVO;
import com.agentguard.vo.TimelineEventVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/timeline")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/project/{projectId}")
    public Result<PageResult<TimelineEventVO>> getProjectTimeline(@PathVariable Long projectId,
                                                                   @RequestParam(defaultValue = "1") long current,
                                                                   @RequestParam(defaultValue = "20") long size,
                                                                   @RequestParam(required = false) String riskLevel) {
        return Result.success(timelineService.getProjectTimeline(projectId, current, size, riskLevel));
    }

    @GetMapping("/project/{projectId}/overview")
    public Result<ProjectSecurityOverviewVO> getProjectOverview(@PathVariable Long projectId) {
        return Result.success(timelineService.getProjectOverview(projectId));
    }

    @GetMapping("/project/{projectId}/high-risk")
    public Result<List<TimelineEventVO>> getHighRiskEvents(@PathVariable Long projectId,
                                                            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(timelineService.getHighRiskEvents(projectId, limit));
    }
}
