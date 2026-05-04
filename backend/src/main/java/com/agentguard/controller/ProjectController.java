package com.agentguard.controller;

import com.agentguard.common.Result;
import com.agentguard.dto.ProjectCreateRequest;
import com.agentguard.dto.ProjectScanRequest;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.ProjectScanService;
import com.agentguard.vo.ProjectInfoVO;
import com.agentguard.vo.ProjectScanVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectInfoService projectInfoService;
    private final ProjectScanService projectScanService;

    public ProjectController(ProjectInfoService projectInfoService, ProjectScanService projectScanService) {
        this.projectInfoService = projectInfoService;
        this.projectScanService = projectScanService;
    }

    @PostMapping
    public Result<Long> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return Result.success(projectInfoService.createProject(request));
    }

    @GetMapping("/{id}")
    public Result<ProjectInfoVO> getProjectDetail(@PathVariable Long id) {
        return Result.success(projectInfoService.getProjectDetail(id));
    }

    @GetMapping
    public Result<Page<ProjectInfoVO>> pageProjects(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.success(projectInfoService.pageProjects(current, size));
    }

    @PostMapping("/scan")
    public Result<ProjectScanVO> scanProject(@Valid @RequestBody ProjectScanRequest request) {
        return Result.success(projectScanService.scanProject(request));
    }
}
