package com.aivanouski.ttstorage.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerException extends BaseRestException {
    public InternalServerException(String code, String message) {
        super(code, message);
    }
}
