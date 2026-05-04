package com.agentguard.service;

import com.agentguard.common.PageResult;
import com.agentguard.entity.ScanTask;
import com.agentguard.vo.ScanResultVO;
import com.agentguard.vo.ScanTaskVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ScanTaskService extends IService<ScanTask> {

    PageResult<ScanTaskVO> pageByProjectId(Long projectId, long current, long size);

    ScanTaskVO getTaskDetail(Long taskId);

    ScanResultVO getTaskResult(Long taskId);
}
