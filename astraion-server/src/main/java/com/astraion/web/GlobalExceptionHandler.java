package com.astraion.web;

import com.astraion.core.engine.CrudException;
import com.astraion.core.metadata.MetadataException;
import com.astraion.core.security.PermissionException;
import com.astraion.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handlePermission(PermissionException e) {
        return ApiResponse.fail(403, e.getMessage());
    }

    @ExceptionHandler(MetadataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMetadata(MetadataException e) {
        return ApiResponse.fail(400, e.getMessage());
    }

    @ExceptionHandler(CrudException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleCrud(CrudException e) {
        return ApiResponse.fail(404, e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleSecurity(SecurityException e) {
        return ApiResponse.fail(403, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneral(Exception e) {
        return ApiResponse.fail(500, "服务器内部错误: " + e.getMessage());
    }
}
