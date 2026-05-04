package com.agentguard.service;

import com.agentguard.dto.ProjectScanRequest;
import com.agentguard.vo.ProjectScanVO;

public interface ProjectScanService {

    ProjectScanVO scanProject(ProjectScanRequest request);
}
