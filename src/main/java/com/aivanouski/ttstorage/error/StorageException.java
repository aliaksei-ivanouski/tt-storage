package com.aivanouski.ttstorage.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class StorageException extends BaseRestException {
    public StorageException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
