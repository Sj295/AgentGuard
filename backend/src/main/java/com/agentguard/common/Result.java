package com.agentguard.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int SUCCESS_CODE = 0;
    private static final int ERROR_CODE = 500;

    private Integer code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, "success", data, LocalDateTime.now());
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(SUCCESS_CODE, message, data, LocalDateTime.now());
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(ERROR_CODE, message, null, LocalDateTime.now());
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null, LocalDateTime.now());
    }
}
