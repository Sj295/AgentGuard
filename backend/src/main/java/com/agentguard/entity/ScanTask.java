package com.agentguard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scan_task")
public class ScanTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String taskType;

    private String status;

    private Integer progress;

    private String resultSummary;

    private String errorMessage;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
