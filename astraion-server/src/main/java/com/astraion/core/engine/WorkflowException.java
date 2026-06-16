package com.astraion.core.engine;

/**
 * 工作流操作异常
 */
public class WorkflowException extends RuntimeException {
    public WorkflowException(String message) {
        super(message);
    }
    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
