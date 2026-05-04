package com.agentguard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_info")
public class ProjectInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String projectName;

    private String projectPath;

    private String projectType;

    private String techStack;

    private Boolean hasGit;

    private Boolean hasAgentsMd;

    private String description;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
