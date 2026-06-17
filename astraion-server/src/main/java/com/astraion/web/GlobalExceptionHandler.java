package com.astraion.web;

import com.astraion.core.engine.CrudException;
import com.astraion.core.metadata.MetadataException;
import com.astraion.core.security.PermissionException;
import com.astraion.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 拦截所有数据库异常，绝不让 SQL 细节泄露到前端
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleDataAccess(DataAccessException e) {
        log.error("Database error", e);
        return ApiResponse.fail(500, "数据操作失败，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneral(Exception e) {
        log.error("Unhandled error", e);
        return ApiResponse.fail(500, "服务器内部错误，请稍后重试");
    }
}
