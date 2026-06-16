package com.astraion.core.datasource;

/**
 * 外部数据源操作异常
 */
public class DataSourceException extends RuntimeException {
    public DataSourceException(String message) {
        super(message);
    }
    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
