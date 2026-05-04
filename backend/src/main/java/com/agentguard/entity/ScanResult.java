package com.agentguard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scan_result")
public class ScanResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long taskId;

    private Integer fileCount;

    private Integer directoryCount;

    private String detectedFiles;

    private String detectedCommands;

    private String sensitiveFiles;

    private String riskLevel;

    private LocalDateTime createdTime;

    @TableLogic
    private Integer deleted;
}
