package com.agentguard.service;

import com.agentguard.dto.MarkdownReportExportRequest;
import com.agentguard.dto.MarkdownReportGenerateRequest;
import com.agentguard.vo.MarkdownReportVO;

public interface MarkdownReportService {

    MarkdownReportVO generate(MarkdownReportGenerateRequest request);

    MarkdownReportVO export(MarkdownReportExportRequest request);
}
