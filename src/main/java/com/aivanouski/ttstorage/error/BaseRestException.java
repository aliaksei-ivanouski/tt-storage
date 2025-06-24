package com.aivanouski.ttstorage.error;

import lombok.Getter;

@Getter
public class BaseRestException extends RuntimeException {
    protected String code;
    protected String message;
    protected Throwable cause;

    public BaseRestException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BaseRestException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.cause = cause;
    }
}
