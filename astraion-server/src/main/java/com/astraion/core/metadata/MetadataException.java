package com.astraion.core.metadata;

/**
 * 元数据操作异常
 */
public class MetadataException extends RuntimeException {
    public MetadataException(String message) {
        super(message);
    }
    public MetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
