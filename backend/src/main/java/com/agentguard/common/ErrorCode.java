package com.agentguard.common;

public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    PROJECT_NOT_FOUND(1001, "项目不存在"),
    TASK_NOT_FOUND(1002, "任务不存在"),
    REPORT_NOT_FOUND(1003, "报告不存在"),
    RULE_NOT_FOUND(1004, "规则不存在"),
    INVALID_AGENT_TYPE(1005, "无效的 Agent 类型"),
    INVALID_RISK_LEVEL(1006, "无效的风险等级"),
    INVALID_REPORT_TYPE(1007, "无效的报告类型"),
    INVALID_PROJECT_PATH(1008, "无效的项目路径"),
    FILE_ALREADY_EXISTS(1009, "目标文件已存在"),
    FILE_WRITE_FAILED(1010, "文件写入失败"),
    GIT_REPOSITORY_NOT_FOUND(1011, "Git 仓库不存在"),
    GIT_COMMAND_FAILED(1012, "Git 命令执行失败"),
    JSON_PARSE_ERROR(1013, "JSON 解析错误"),
    SYSTEM_ERROR(5000, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
