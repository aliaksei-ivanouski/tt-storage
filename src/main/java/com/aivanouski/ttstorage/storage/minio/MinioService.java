package com.aivanouski.ttstorage.storage.minio;

import com.aivanouski.ttstorage.error.StorageException;
import com.aivanouski.ttstorage.error.BadRequestException;
import com.aivanouski.ttstorage.file.FilenameMetadata;
import com.aivanouski.ttstorage.storage.StorageService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static com.aivanouski.ttstorage.error.model.ErrorCodes.FILENAME_IS_ABSENT_ERROR;
import static com.aivanouski.ttstorage.error.model.ErrorCodes.MINIO_CONNECTION_ERROR;
import static com.aivanouski.ttstorage.error.model.ErrorCodes.MINIO_SECURITY_ERROR;
import static com.aivanouski.ttstorage.error.model.ErrorCodes.MINIO_UNEXPECTED_EXCEPTION;
import static com.aivanouski.ttstorage.error.model.ErrorCodes.FILE_UPLOAD_ERROR;
import static com.aivanouski.ttstorage.error.model.ErrorCodes.DELETE_FILE_FROM_STORAGE_ERROR;
import static com.aivanouski.ttstorage.util.FilenameUtil.buildInitialFilenameMetadata;

@Slf4j
@Service
public class MinioService implements StorageService {

    private static final int UPLOAD_PART_SIZE = 10 * 1024 * 1024;

    private final MinioClient minioClient;

    private final String bucketName;

    public MinioService(@Value("${minio.bucket-name}") String bucketName, MinioClient minioClient) {
        this.bucketName = bucketName;
        this.minioClient = minioClient;
    }

    @PostConstruct
    public void initializeBucket() {
        log.info("Initializing Minio Bucket, bucket name: {}", bucketName);
        try {
            log.info("MinIO Client: {}", minioClient);
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' created successfully.", bucketName);
            } else {
                log.warn("MinIO bucket '{}' already exists.", bucketName);
            }
        } catch (MinioException | IOException e) {
            log.error("MinIO error during bucket initialization: {}", e.getMessage());
            throw new StorageException(MINIO_CONNECTION_ERROR,
                    "MinIO connection or bucket creation failed: " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Security error during bucket initialization: {}", e.getMessage());
            throw new StorageException(MINIO_SECURITY_ERROR,
                    "Security error with MinIO client: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during MinIO bucket initialization: {}", e.getMessage());
            throw new StorageException(MINIO_UNEXPECTED_EXCEPTION,
                    "Failed to initialize MinIO bucket due to an unexpected error.", e);
        }
    }

    public FilenameMetadata uploadFile(MultipartFile file, UUID fileId) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BadRequestException(FILENAME_IS_ABSENT_ERROR, "request does not contain a file name");
        }

        try (final InputStream inputStream = file.getInputStream()) {
            FilenameMetadata filenameMetadata = buildInitialFilenameMetadata(fileId, filename, inputStream);
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filenameMetadata.getMappedFullName())
                    .stream(inputStream, -1, UPLOAD_PART_SIZE)
                    .build();
            minioClient.putObject(args);
            return filenameMetadata;
        } catch (MinioException e) {
            log.error("MinIO error during file upload: {}", e.getMessage());
            throw new StorageException(MINIO_CONNECTION_ERROR, "MinIO error during file upload: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("I/O error during file upload: {}", e.getMessage());
            throw new StorageException(FILE_UPLOAD_ERROR, "I/O error during file upload: " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Security error during file upload: {}", e.getMessage());
            throw new StorageException(MINIO_SECURITY_ERROR, "Security error during file upload: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during file upload: {}", e.getMessage());
            throw new StorageException(MINIO_UNEXPECTED_EXCEPTION,
                    "An unexpected error occurred during file upload: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String minioObjectName) {
        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(minioObjectName)
                    .build();
            minioClient.removeObject(args);
        } catch (MinioException e) {
            log.error("MinIO error during file deletion: {}", e.getMessage());
            throw new StorageException(MINIO_CONNECTION_ERROR, "MinIO error during file deletion: " + e.getMessage(),
                    e);
        } catch (IOException e) {
            log.error("I/O error during file deletion: {}", e.getMessage());
            throw new StorageException(DELETE_FILE_FROM_STORAGE_ERROR,
                    "I/O error during file deletion: " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Security error during file deletion: {}", e.getMessage());
            throw new StorageException(MINIO_SECURITY_ERROR, "Security error during file deletion: " + e.getMessage(),
                    e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during file deletion: {}", e.getMessage());
            throw new StorageException(MINIO_UNEXPECTED_EXCEPTION,
                    "An unexpected error occurred during file deletion: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getFile(String minioObjectName) {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(minioObjectName)
                .build();
        try {
            return minioClient.getObject(args);
        } catch (MinioException e) {
            log.error("MinIO error during get file: '{}' from bucket: '{}'", minioObjectName, bucketName);
            throw new StorageException(MINIO_CONNECTION_ERROR,
                    "MinIO error during get file from storage: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("I/O error during get file: '{}' from bucket: '{}'", minioObjectName, bucketName);
            throw new StorageException(FILE_UPLOAD_ERROR, "I/O error during get file from storage: " + e.getMessage(),
                    e);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Security error during get file: '{}' from bucket: '{}'", minioObjectName, bucketName);
            throw new StorageException(MINIO_SECURITY_ERROR,
                    "Security error during get file from storage: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during get file: '{}' from bucket: '{}'", minioObjectName,
                    bucketName);
            throw new StorageException(MINIO_UNEXPECTED_EXCEPTION,
                    "An unexpected error occurred during get file from storage: " + e.getMessage(), e);
        }
    }
}
