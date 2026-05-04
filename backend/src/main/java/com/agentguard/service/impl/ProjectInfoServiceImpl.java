package com.agentguard.service.impl;

import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.dto.ProjectCreateRequest;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.mapper.ProjectInfoMapper;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.vo.ProjectInfoVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectInfoServiceImpl extends ServiceImpl<ProjectInfoMapper, ProjectInfo> implements ProjectInfoService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(ProjectCreateRequest request) {
        ProjectInfo projectInfo = new ProjectInfo();
        projectInfo.setProjectName(request.getProjectName());
        projectInfo.setProjectPath(request.getProjectPath());
        projectInfo.setProjectType(request.getProjectType() == null || request.getProjectType().isBlank()
                ? "UNKNOWN"
                : request.getProjectType());
        projectInfo.setTechStack(request.getTechStack() == null || request.getTechStack().isBlank()
                ? "[]"
                : request.getTechStack());
        projectInfo.setHasGit(Boolean.TRUE.equals(request.getHasGit()));
        projectInfo.setHasAgentsMd(Boolean.TRUE.equals(request.getHasAgentsMd()));
        projectInfo.setDescription(request.getDescription());
        this.save(projectInfo);
        return projectInfo.getId();
    }

    @Override
    public ProjectInfoVO getProjectDetail(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Project id cannot be null");
        }
        ProjectInfo projectInfo = this.getById(id);
        if (projectInfo == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return toProjectInfoVO(projectInfo);
    }

    @Override
    public Page<ProjectInfoVO> pageProjects(long current, long size) {
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Pagination parameters must be greater than 0");
        }
        Page<ProjectInfo> pageParam = new Page<>(current, size);
        Page<ProjectInfo> entityPage = this.page(
                pageParam,
                Wrappers.<ProjectInfo>lambdaQuery().orderByDesc(ProjectInfo::getCreatedTime)
        );
        Page<ProjectInfoVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toProjectInfoVO).toList());
        return voPage;
    }

    private ProjectInfoVO toProjectInfoVO(ProjectInfo projectInfo) {
        ProjectInfoVO vo = new ProjectInfoVO();
        vo.setId(projectInfo.getId());
        vo.setProjectName(projectInfo.getProjectName());
        vo.setProjectPath(projectInfo.getProjectPath());
        vo.setProjectType(projectInfo.getProjectType());
        vo.setTechStack(projectInfo.getTechStack());
        vo.setHasGit(projectInfo.getHasGit());
        vo.setHasAgentsMd(projectInfo.getHasAgentsMd());
        vo.setDescription(projectInfo.getDescription());
        vo.setCreatedTime(projectInfo.getCreatedTime());
        vo.setUpdatedTime(projectInfo.getUpdatedTime());
        return vo;
    }
}
