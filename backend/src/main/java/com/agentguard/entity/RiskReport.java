package com.agentguard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("risk_report")
public class RiskReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String reportType;

    private String riskLevel;

    private Integer riskScore;

    private String summary;

    private String riskItems;

    private String suggestions;

    private String payloadJson;

    private LocalDateTime createdTime;

    @TableLogic
    private Integer deleted;
}
