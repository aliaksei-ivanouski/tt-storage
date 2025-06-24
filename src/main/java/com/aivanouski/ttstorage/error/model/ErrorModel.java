package com.aivanouski.ttstorage.error.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "API error response")
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class ErrorModel {
    @Schema(description = "Error time", example = "2025-06-20T05:52:16.793Z ...")
    private String timestamp;

    @Schema(description = "Error code", example = "error.file.not.found ...")
    private String code;

    @Schema(description = "HTTP status code", example = "400 | 404 | 500 ...")
    private int status;

    @Schema(description = "Api path", example = "/api/v1/storage/upload ...")
    private String path;

    @Schema(description = "Error name", example = "NotFoundException | StorageException ...")
    private String error;

    @Schema(description = "Error message", example = "Access denied | File duplicated ...")
    private String message;
}