package com.aivanouski.ttstorage.error.model;

public interface ErrorCodes {
    String INTERNAL_SERVER_ERROR = "error.internal.server";
    String FILENAME_IS_ABSENT_ERROR = "error.filename.is.absent";
    String NOT_FOUND_OR_ACCESS_DENIED_ERROR = "error.file.not.found.or.access.denied";

    String SAME_FILE_ERROR = "error.same.file";
    String FILE_UPLOAD_ERROR = "error.file.upload";

    String DELETE_FILE_FROM_STORAGE_ERROR = "error.delete.from.storage";
    String DELETE_FILE_FROM_DB_ERROR = "error.delete.from.db";

    String PARSE_VALIDATION_ERROR = "error.parse.validation";
    String PARSE_URI_ERROR = "error.parse.uri";
    String PARSE_URL_ERROR = "error.parse.url";

    String MINIO_CONNECTION_ERROR = "error.minio.connection";
    String MINIO_SECURITY_ERROR = "error.minio.security";
    String MINIO_UNEXPECTED_EXCEPTION = "error.minio.unexpected";

    // Added for error handler mapping
    String FILE_ABSENT_ERROR = "error.file.absent";
    String INVALID_JSON_ERROR = "error.invalid.json";
    String VALIDATION_FAILED_ERROR = "error.validation.failed";
    String REQUEST_PARAM_ABSENT_ERROR = "error.request.param.absent";
    String MULTIPART_PARSING_ERROR = "error.multipart.parsing";
}
