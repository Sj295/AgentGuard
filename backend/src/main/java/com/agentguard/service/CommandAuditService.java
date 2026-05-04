package com.agentguard.service;

import com.agentguard.common.PageResult;
import com.agentguard.dto.CommandAuditRequest;
import com.agentguard.vo.CommandAuditVO;

public interface CommandAuditService {

    CommandAuditVO auditCommands(CommandAuditRequest request);

    PageResult<CommandAuditVO> pageProjectReports(Long projectId, long current, long size);
}
