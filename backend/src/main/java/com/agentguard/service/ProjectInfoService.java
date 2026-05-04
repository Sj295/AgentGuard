package com.agentguard.service;

import com.agentguard.dto.ProjectCreateRequest;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.vo.ProjectInfoVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ProjectInfoService extends IService<ProjectInfo> {

    Long createProject(ProjectCreateRequest request);

    ProjectInfoVO getProjectDetail(Long id);

    Page<ProjectInfoVO> pageProjects(long current, long size);
}
