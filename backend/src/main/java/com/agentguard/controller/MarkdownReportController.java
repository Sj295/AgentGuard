package com.agentguard.controller;

import com.agentguard.common.Result;
import com.agentguard.dto.MarkdownReportExportRequest;
import com.agentguard.dto.MarkdownReportGenerateRequest;
import com.agentguard.service.MarkdownReportService;
import com.agentguard.vo.MarkdownReportVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/markdown")
public class MarkdownReportController {

    private final MarkdownReportService markdownReportService;

    public MarkdownReportController(MarkdownReportService markdownReportService) {
        this.markdownReportService = markdownReportService;
    }

    @PostMapping("/generate")
    public Result<MarkdownReportVO> generate(@Valid @RequestBody MarkdownReportGenerateRequest request) {
        return Result.success(markdownReportService.generate(request));
    }

    @PostMapping("/export")
    public Result<MarkdownReportVO> export(@Valid @RequestBody MarkdownReportExportRequest request) {
        return Result.success(markdownReportService.export(request));
    }
}
