package com.agentguard.service.impl;

import com.agentguard.entity.RiskReport;
import com.agentguard.mapper.RiskReportMapper;
import com.agentguard.service.RiskReportService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RiskReportServiceImpl extends ServiceImpl<RiskReportMapper, RiskReport> implements RiskReportService {
}
