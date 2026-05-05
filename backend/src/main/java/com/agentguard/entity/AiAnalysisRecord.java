package com.agentguard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_analysis_record")
public class AiAnalysisRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String analysisType;

    private Long sourceReportId;

    private String provider;

    private String model;

    private Boolean mocked;

    private Boolean success;

    private Long latencyMs;

    private String inputSummary;

    private String outputContent;

    private String errorMessage;

    private LocalDateTime createdTime;

    @TableLogic
    private Integer deleted;
}
