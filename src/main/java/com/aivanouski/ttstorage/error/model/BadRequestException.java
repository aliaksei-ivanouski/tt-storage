package com.aivanouski.ttstorage.error.model;

import com.aivanouski.ttstorage.error.BaseRestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends BaseRestException {
    public BadRequestException(String code, String message) {
        super(code, message);
    }

    public BadRequestException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
