package com.agentguard.service;

import com.agentguard.common.PageResult;
import com.agentguard.entity.ScanResult;
import com.agentguard.vo.ScanResultDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ScanResultService extends IService<ScanResult> {

    ScanResultDetailVO getLatestByProjectId(Long projectId);

    PageResult<ScanResultDetailVO> pageByProjectId(Long projectId, long current, long size);
}
