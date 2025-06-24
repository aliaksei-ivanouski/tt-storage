package com.aivanouski.ttstorage.storage;

import com.aivanouski.ttstorage.file.FilenameMetadata;
import com.aivanouski.ttstorage.error.StorageException;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing file storage operations.
 *
 * <p>
 * This interface defines the contract for file storage operations, providing
 * a unified API for uploading, downloading, deleting, and listing files
 * regardless
 * of the underlying storage implementation (e.g., MinIO, AWS S3, etc.).
 *
 * <p>
 * The service is designed to work with unique object names (typically UUIDs)
 * to ensure no naming conflicts occur when storing files, regardless of their
 * original filenames. This approach provides several benefits:
 * <ul>
 * <li>Prevents filename collisions between different users</li>
 * <li>Eliminates issues with special characters in filenames</li>
 * <li>Provides consistent and predictable object naming</li>
 * <li>Enables efficient storage and retrieval operations</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>File upload with automatic unique name generation</li>
 * <li>File download and retrieval</li>
 * <li>File deletion and cleanup</li>
 * <li>Storage inventory and listing</li>
 * <li>Error handling and exception management</li>
 * </ul>
 *
 * <p>
 * Implementation notes:
 * <ul>
 * <li>All methods should be thread-safe</li>
 * <li>Exceptions should be wrapped in {@link StorageException} with appropriate
 * error codes</li>
 * <li>File operations should be atomic where possible</li>
 * <li>Streams should be properly closed to prevent resource leaks</li>
 * <li>Error codes should be used from
 * {@link com.aivanouski.ttstorage.error.model.ErrorCodes}</li>
 * </ul>
 *
 * @author Aliaksei Ivanouski
 * @version 1.0.0
 * @see com.aivanouski.ttstorage.storage.minio.MinioService
 * @see com.aivanouski.ttstorage.file.FilenameMetadata
 * @see com.aivanouski.ttstorage.error.StorageException
 * @see com.aivanouski.ttstorage.error.model.ErrorCodes
 * @see org.springframework.web.multipart.MultipartFile
 * @since 1.0.0
 */
public interface StorageService {

    /**
     * Uploads a file to the storage system.
     *
     * <p>
     * This method handles the complete file upload process, including:
     * <ul>
     * <li>Generating a unique object name (UUID) for storage</li>
     * <li>Validating the uploaded file</li>
     * <li>Storing the file content in the underlying storage system</li>
     * <li>Returning metadata about the stored file</li>
     * </ul>
     *
     * <p>
     * The method ensures that no naming conflicts occur by using a UUID-based
     * naming scheme, regardless of the original filename. This allows multiple
     * files with the same name to be uploaded without conflicts.
     *
     * <p>
     * Performance considerations:
     * <ul>
     * <li>Large files may be uploaded in chunks for better performance</li>
     * <li>Upload progress can be monitored through the returned metadata</li>
     * <li>Network timeouts should be handled gracefully</li>
     * </ul>
     *
     * @param file The multipart file received from the client. Must not be null
     *             and must contain valid file data.
     * @return A {@link FilenameMetadata} object containing information about the
     * uploaded file, including the original filename and the unique
     * object name used for storage.
     * @throws StorageException         if the upload fails due to storage system
     *                                  errors,
     *                                  network issues, or invalid file data. The
     *                                  specific cause will be
     *                                  included in the exception message and
     *                                  appropriate error code.
     * @throws IllegalArgumentException if the file parameter is null or empty
     * @see com.aivanouski.ttstorage.file.FilenameMetadata
     * @see com.aivanouski.ttstorage.error.StorageException
     */
    FilenameMetadata uploadFile(MultipartFile file);

    /**
     * Uploads a file to the storage system with a specific UUID.
     *
     * <p>
     * This method is similar to {@link #uploadFile(MultipartFile)} but allows
     * specifying a custom UUID for the object name, ensuring consistency between
     * the database fileId and storage object name.
     *
     * @param file   The multipart file received from the client. Must not be null
     *               and must contain valid file data.
     * @param fileId The UUID to use for the object name in storage.
     * @return A {@link FilenameMetadata} object containing information about the
     * uploaded file, including the original filename and the unique
     * object name used for storage.
     * @throws StorageException         if the upload fails due to storage system
     *                                  errors, network issues, or invalid file
     *                                  data.
     * @throws IllegalArgumentException if the file parameter is null or empty
     * @see com.aivanouski.ttstorage.file.FilenameMetadata
     * @see com.aivanouski.ttstorage.error.StorageException
     */
    FilenameMetadata uploadFile(MultipartFile file, UUID fileId);

    /**
     * Downloads a file from the storage system.
     *
     * <p>
     * This method retrieves a file from storage using its unique object name
     * and returns an input stream for reading the file content. The caller is
     * responsible for properly closing the returned stream to prevent resource
     * leaks.
     *
     * <p>
     * The method performs the following operations:
     * <ul>
     * <li>Validates the provided object name</li>
     * <li>Locates the file in the storage system</li>
     * <li>Opens a stream for reading the file content</li>
     * <li>Returns the stream for further processing</li>
     * </ul>
     *
     * <p>
     * Usage example:
     *
     * <pre>{@code
     * try (InputStream stream = storageService.downloadFile(objectName)) {
     *     // Process the file content
     *     byte[] content = stream.readAllBytes();
     * }
     * }</pre>
     *
     * @param minioObjectName The unique object name of the file in storage.
     *                        Must be a valid, non-null string that corresponds
     *                        to an existing file.
     * @return An {@link InputStream} containing the file content. The stream
     * must be closed by the caller to prevent resource leaks.
     * @throws StorageException         if the download fails due to storage system
     *                                  errors,
     *                                  network issues, or if the file is not found.
     *                                  The specific cause
     *                                  will be included in the exception message
     *                                  and appropriate error code.
     * @throws IllegalArgumentException if the object name is null or empty
     * @see java.io.InputStream
     * @see com.aivanouski.ttstorage.error.StorageException
     */
    InputStream downloadFile(String minioObjectName);

    /**
     * Deletes a file from the storage system.
     *
     * <p>
     * This method permanently removes a file from storage using its unique
     * object name. The deletion operation is irreversible, so care should be
     * taken to ensure the correct file is being deleted.
     *
     * <p>
     * The method performs the following operations:
     * <ul>
     * <li>Validates the provided object name</li>
     * <li>Locates the file in the storage system</li>
     * <li>Permanently removes the file and its metadata</li>
     * <li>Confirms successful deletion</li>
     * </ul>
     *
     * <p>
     * Important considerations:
     * <ul>
     * <li>The deletion operation is permanent and cannot be undone</li>
     * <li>If the file doesn't exist, the operation may succeed silently</li>
     * <li>Storage space is reclaimed immediately after deletion</li>
     * <li>Concurrent access to the same file during deletion may cause issues</li>
     * </ul>
     *
     * @param minioObjectName The unique object name of the file to delete.
     *                        Must be a valid, non-null string.
     * @throws StorageException         if the deletion fails due to storage system
     *                                  errors,
     *                                  network issues, or insufficient permissions.
     *                                  The specific cause
     *                                  will be included in the exception message
     *                                  and appropriate error code.
     * @throws IllegalArgumentException if the object name is null or empty
     * @see com.aivanouski.ttstorage.error.StorageException
     */
    void deleteFile(String minioObjectName);

    /**
     * Lists all objects in the configured storage bucket.
     *
     * <p>
     * This method retrieves a list of all unique object names currently
     * stored in the storage system. This can be useful for inventory purposes,
     * cleanup operations, or system administration tasks.
     *
     * <p>
     * The method performs the following operations:
     * <ul>
     * <li>Connects to the storage system</li>
     * <li>Retrieves all object names from the configured bucket</li>
     * <li>Returns the list of object names</li>
     * </ul>
     *
     * <p>
     * Performance considerations:
     * <ul>
     * <li>For large storage buckets, this operation may take significant time</li>
     * <li>The returned list may be paginated depending on the implementation</li>
     * <li>Network bandwidth usage should be considered for large buckets</li>
     * <li>Caching may be implemented for frequently accessed lists</li>
     * </ul>
     *
     * @return A list of unique object names (strings) representing all files
     * currently stored in the system. The list may be empty if no
     * files are stored, but will never be null.
     * @throws StorageException if listing fails due to storage system errors,
     *                          network issues, or insufficient permissions. The
     *                          specific cause
     *                          will be included in the exception message and
     *                          appropriate error code.
     * @see java.util.List
     * @see com.aivanouski.ttstorage.error.StorageException
     */
    List<String> listFiles();

    /**
     * Retrieves a file from the storage system.
     *
     * <p>
     * This method is similar to {@link #downloadFile(String)} but may provide
     * additional functionality or different behavior depending on the
     * implementation.
     * It retrieves a file from storage using its unique object name and returns
     * an input stream for reading the file content.
     *
     * <p>
     * The method performs the following operations:
     * <ul>
     * <li>Validates the provided object name</li>
     * <li>Locates the file in the storage system</li>
     * <li>Opens a stream for reading the file content</li>
     * <li>Returns the stream for further processing</li>
     * </ul>
     *
     * <p>
     * Usage example:
     *
     * <pre>{@code
     * try (InputStream stream = storageService.getFile(objectName)) {
     *     // Process the file content
     *     Files.copy(stream, Paths.get("downloaded-file"));
     * }
     * }</pre>
     *
     * @param minioObjectName The unique object name of the file to retrieve.
     *                        Must be a valid, non-null string that corresponds
     *                        to an existing file.
     * @return An {@link InputStream} containing the file content. The stream
     * must be closed by the caller to prevent resource leaks.
     * @throws StorageException         if the retrieval fails due to storage system
     *                                  errors,
     *                                  network issues, or if the file is not found.
     *                                  The specific cause
     *                                  will be included in the exception message
     *                                  and appropriate error code.
     * @throws IllegalArgumentException if the object name is null or empty
     * @see java.io.InputStream
     * @see #downloadFile(String)
     * @see com.aivanouski.ttstorage.error.StorageException
     */
    InputStream getFile(String minioObjectName);
}
