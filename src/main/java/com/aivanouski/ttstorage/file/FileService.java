package com.aivanouski.ttstorage.file;

import com.aivanouski.ttstorage.error.StorageException;
import com.aivanouski.ttstorage.storage.TTFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

/**
 * Service interface for managing file operations in the TT Storage system.
 *
 * <p>
 * This interface defines the business logic layer for file management
 * operations,
 * providing a high-level API for file upload, download, management, and
 * retrieval.
 * The service handles both the storage layer operations and the metadata
 * management
 * in the database.
 *
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>File upload with metadata creation and storage</li>
 * <li>File retrieval with access control validation</li>
 * <li>File renaming and metadata updates</li>
 * <li>File deletion with cleanup of both storage and metadata</li>
 * <li>File listing with pagination and filtering</li>
 * <li>User-based access control and visibility management</li>
 * <li>Tag-based file organization and search</li>
 * </ul>
 *
 * <p>
 * Security and access control:
 * <ul>
 * <li>All operations require valid user identification</li>
 * <li>Users can only access their own files (except for public files)</li>
 * <li>File visibility controls (PUBLIC/PRIVATE) are enforced</li>
 * <li>Tag-based filtering supports both user and public file searches</li>
 * </ul>
 *
 * <p>
 * Data integrity features:
 * <ul>
 * <li>MD5 checksum validation for file integrity</li>
 * <li>Unique file identification using UUIDs</li>
 * <li>Automatic MIME type detection and validation</li>
 * <li>Duplicate file detection based on content and user</li>
 * <li>Atomic operations where possible</li>
 * </ul>
 *
 * <p>
 * Implementation notes:
 * <ul>
 * <li>All methods should be thread-safe</li>
 * <li>Database transactions should be used for metadata operations</li>
 * <li>Storage operations should be coordinated with metadata updates</li>
 * <li>Error handling should use appropriate exception types</li>
 * <li>Pagination should be efficient for large datasets</li>
 * </ul>
 *
 * @author Aliaksei Ivanouski
 * @version 1.0.0
 * @see com.aivanouski.ttstorage.file.DefaultFileService
 * @see com.aivanouski.ttstorage.file.FileDTO
 * @see com.aivanouski.ttstorage.file.File
 * @see com.aivanouski.ttstorage.storage.TTFile
 * @see com.aivanouski.ttstorage.error.StorageException
 * @since 1.0.0
 */
public interface FileService {

    /**
     * Uploads a file to the storage system with associated metadata.
     *
     * <p>
     * This method handles the complete file upload process, including:
     * <ul>
     * <li>Validating the uploaded file and user permissions</li>
     * <li>Generating a unique file ID and storage object name</li>
     * <li>Computing MD5 checksum for file integrity</li>
     * <li>Detecting MIME type and file extension</li>
     * <li>Storing the file in the underlying storage system</li>
     * <li>Creating metadata records in the database</li>
     * <li>Applying user-specified tags and visibility settings</li>
     * </ul>
     *
     * <p>
     * The method ensures data integrity by:
     * <ul>
     * <li>Preventing duplicate file uploads for the same user</li>
     * <li>Validating file content and size</li>
     * <li>Ensuring proper error handling and rollback</li>
     * </ul>
     *
     * <p>
     * Usage example:
     *
     * <pre>{@code
     * MultipartFile file = // ... file from request
     * Set<String> tags = Set.of("document", "important");
     * FileDTO result = fileService.uploadFile(
     *     userId, "application/pdf", File.Visibility.PRIVATE, tags, file
     * );
     * }</pre>
     *
     * @param userId      The unique identifier of the user uploading the file.
     *                    Must not be null.
     * @param contentType The MIME type of the file. If null, will be auto-detected.
     * @param visibility  The visibility setting for the file (PUBLIC or PRIVATE).
     *                    Must not be null.
     * @param tags        A set of tags to associate with the file. May be null or
     *                    empty.
     *                    Tags are automatically converted to lowercase for
     *                    consistency.
     * @param file        The multipart file to upload. Must not be null and must
     *                    contain
     *                    valid file data.
     * @return A {@link FileDTO} containing the uploaded file's metadata including
     * the generated file ID, download link, and all associated information.
     * @throws StorageException         if the upload fails due to storage system
     *                                  errors,
     *                                  database errors, or validation failures. The
     *                                  specific cause will be
     *                                  included in the exception message.
     * @throws IllegalArgumentException if any required parameters are null or
     *                                  invalid
     * @see com.aivanouski.ttstorage.file.FileDTO
     * @see com.aivanouski.ttstorage.file.File.Visibility
     * @see com.aivanouski.ttstorage.error.StorageException
     */
    FileDTO uploadFile(UUID userId,
                       String contentType,
                       File.Visibility visibility,
                       Set<String> tags,
                       MultipartFile file) throws StorageException;

    /**
     * Renames a file owned by the specified user.
     *
     * <p>
     * This method allows users to rename their files while preserving all other
     * metadata and the actual file content. The operation updates only the display
     * name of the file, not the underlying storage object name.
     *
     * <p>
     * The method performs the following operations:
     * <ul>
     * <li>Validates that the user owns the specified file</li>
     * <li>Updates the filename in the database metadata</li>
     * <li>Preserves the file extension and other properties</li>
     * <li>Updates the modification timestamp</li>
     * </ul>
     *
     * <p>
     * Security considerations:
     * <ul>
     * <li>Only the file owner can rename their files</li>
     * <li>The operation is atomic and consistent</li>
     * <li>File access permissions remain unchanged</li>
     * </ul>
     *
     * @param userId  The unique identifier of the user who owns the file.
     *                Must not be null.
     * @param fileId  The unique identifier of the file to rename.
     *                Must not be null and must exist.
     * @param newFile The new filename for the file. Must not be null or empty.
     *                The file extension will be preserved from the original file.
     * @return A {@link FileDTO} containing the updated file metadata with the new
     * filename and updated timestamp.
     * @throws StorageException         if the rename operation fails due to
     *                                  database errors,
     *                                  access permission issues, or if the file is
     *                                  not found. The specific
     *                                  cause will be included in the exception
     *                                  message.
     * @throws IllegalArgumentException if any parameters are null or invalid
     * @see com.aivanouski.ttstorage.file.FileDTO
     * @see com.aivanouski.ttstorage.error.StorageException
     */
    FileDTO renameFile(UUID userId, UUID fileId, String newFile) throws StorageException;

    /**
     * Retrieves a file with its content for the specified user.
     *
     * <p>
     * This method provides access to both file metadata and content for users
     * who have permission to access the file. The method validates user permissions
     * before providing access to the file content.
     *
     * <p>
     * The method performs the following operations:
     * <ul>
     * <li>Validates that the user has access to the specified file</li>
     * <li>Retrieves file metadata from the database</li>
     * <li>Downloads the file content from the storage system</li>
     * <li>Returns both metadata and content stream</li>
     * </ul>
     *
     * <p>
     * Access control rules:
     * <ul>
     * <li>Users can access their own files (regardless of visibility)</li>
     * <li>Public files can be accessed by any authenticated user</li>
     * <li>Private files can only be accessed by their owners</li>
     * </ul>
     *
     * <p>
     * Usage example:
     *
     * <pre>{@code
     * TTFile file = fileService.getFile(userId, fileId);
     * // Use file.getMetadata() for file information
     * // Use file.getInputStream() for file content
     * }</pre>
     *
     * @param userId The unique identifier of the user requesting the file.
     *               Must not be null.
     * @param fileId The unique identifier of the file to retrieve.
     *               Must not be null and must exist.
     * @return A {@link TTFile} object containing both the file metadata and an
     * input stream for the file content. The stream must be closed by
     * the caller to prevent resource leaks.
     * @throws StorageException         if the retrieval fails due to storage system
     *                                  errors,
     *                                  database errors, access permission issues,
     *                                  or if the file is not found.
     *                                  The specific cause will be included in the
     *                                  exception message.
     * @throws IllegalArgumentException if any parameters are null or invalid
     * @see com.aivanouski.ttstorage.storage.TTFile
     * @see com.aivanouski.ttstorage.error.StorageException
     */
    TTFile getFile(UUID userId, UUID fileId) throws StorageException;

    /**
     * Lists all public files with optional tag filtering and pagination.
     *
     * <p>
     * This method provides access to all public files in the system, allowing
     * users to discover and browse publicly available content. The method supports
     * filtering by tags and pagination for efficient handling of large datasets.
     *
     * <p>
     * The method performs the following operations:
     * <ul>
     * <li>Retrieves public files from the database</li>
     * <li>Applies tag-based filtering if specified</li>
     * <li>Applies pagination parameters</li>
     * <li>Returns file metadata without content</li>
     * </ul>
     *
     * <p>
     * Filtering behavior:
     * <ul>
     * <li>If no tags are provided, all public files are returned</li>
     * <li>If tags are provided, only files with matching tags are returned</li>
     * <li>Tag matching is case-insensitive</li>
     * <li>Files must have ALL specified tags to be included</li>
     * </ul>
     *
     * <p>
     * Usage example:
     *
     * <pre>{@code
     * Set<String> tags = Set.of("document", "public");
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
     * Page<FileDTO> files = fileService.listPublicFiles(tags, pageable);
     * }</pre>
     *
     * @param tags     A set of tags to filter files by. If null or empty, no tag
     *                 filtering is applied and all public files are returned.
     *                 Tags are automatically converted to lowercase for matching.
     * @param pageable Pagination and sorting parameters. Must not be null.
     *                 Controls the number of results and their ordering.
     * @return A {@link Page} of {@link FileDTO} objects containing metadata for
     * public files that match the specified criteria. The page includes
     * pagination information and total count.
     * @throws IllegalArgumentException if pageable parameter is null
     * @see org.springframework.data.domain.Page
     * @see org.springframework.data.domain.Pageable
     * @see com.aivanouski.ttstorage.file.FileDTO
     */
    Page<FileDTO> listPublicFiles(Set<String> tags, Pageable pageable);

    /**
     * Lists files owned by a specific user with optional tag filtering and
     * pagination.
     *
     * <p>
     * This method provides access to files owned by a specific user, including
     * both public and private files. The method supports filtering by tags and
     * pagination for efficient handling of large datasets.
     *
     * <p>
     * The method performs the following operations:
     * <ul>
     * <li>Retrieves files owned by the specified user from the database</li>
     * <li>Applies tag-based filtering if specified</li>
     * <li>Applies pagination parameters</li>
     * <li>Returns file metadata without content</li>
     * </ul>
     *
     * <p>
     * Access control:
     * <ul>
     * <li>Users can only list their own files</li>
     * <li>Both public and private files are included in the results</li>
     * <li>No access to other users' files is provided</li>
     * </ul>
     *
     * <p>
     * Filtering behavior:
     * <ul>
     * <li>If no tags are provided, all user files are returned</li>
     * <li>If tags are provided, only files with matching tags are returned</li>
     * <li>Tag matching is case-insensitive</li>
     * <li>Files must have ALL specified tags to be included</li>
     * </ul>
     *
     * <p>
     * Usage example:
     *
     * <pre>{@code
     * Set<String> tags = Set.of("work", "important");
     * Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());
     * Page<FileDTO> files = fileService.listUserFiles(userId, tags, pageable);
     * }</pre>
     *
     * @param userId   The unique identifier of the user whose files to list.
     *                 Must not be null.
     * @param tags     A set of tags to filter files by. If null or empty, no tag
     *                 filtering is applied and all user files are returned.
     *                 Tags are automatically converted to lowercase for matching.
     * @param pageable Pagination and sorting parameters. Must not be null.
     *                 Controls the number of results and their ordering.
     * @return A {@link Page} of {@link FileDTO} objects containing metadata for
     * files owned by the specified user that match the specified criteria.
     * The page includes pagination information and total count.
     * @throws IllegalArgumentException if userId or pageable parameters are null
     * @see org.springframework.data.domain.Page
     * @see org.springframework.data.domain.Pageable
     * @see com.aivanouski.ttstorage.file.FileDTO
     */
    Page<FileDTO> listUserFiles(UUID userId, Set<String> tags, Pageable pageable);

    /**
     * Deletes a file owned by the specified user.
     *
     * <p>
     * This method permanently removes a file from both the storage system and
     * the database. The deletion operation is irreversible, so care should be
     * taken to ensure the correct file is being deleted.
     *
     * <p>
     * The method performs the following operations:
     * <ul>
     * <li>Validates that the user owns the specified file</li>
     * <li>Removes the file from the storage system</li>
     * <li>Deletes the file metadata from the database</li>
     * <li>Handles cleanup of associated resources</li>
     * </ul>
     *
     * <p>
     * Security and data integrity:
     * <ul>
     * <li>Only the file owner can delete their files</li>
     * <li>The operation is atomic - either both storage and metadata are deleted,
     * or neither is deleted</li>
     * <li>Storage space is reclaimed immediately</li>
     * <li>Database records are permanently removed</li>
     * </ul>
     *
     * <p>
     * Error handling:
     * <ul>
     * <li>If storage deletion fails, the database record is preserved</li>
     * <li>If database deletion fails, the storage file may remain (cleanup
     * needed)</li>
     * <li>Appropriate exceptions are thrown for different failure scenarios</li>
     * </ul>
     *
     * @param userId The unique identifier of the user who owns the file.
     *               Must not be null.
     * @param fileId The unique identifier of the file to delete.
     *               Must not be null and must exist.
     * @throws StorageException         if the deletion fails due to storage system
     *                                  errors,
     *                                  database errors, access permission issues,
     *                                  or if the file is not found.
     *                                  The specific cause will be included in the
     *                                  exception message.
     * @throws IllegalArgumentException if any parameters are null or invalid
     * @see com.aivanouski.ttstorage.error.StorageException
     */
    void deleteFile(UUID userId, UUID fileId);
}
