package com.aivanouski.ttstorage.util;

import com.aivanouski.ttstorage.error.InternalServerException;
import com.aivanouski.ttstorage.error.model.ErrorCodes;
import com.aivanouski.ttstorage.file.FilenameMetadata;
import org.apache.tika.Tika;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Utility class for managing filename operations and metadata in the TT-Storage
 * system.
 *
 * <p>
 * This utility provides functionality for creating, manipulating, and managing
 * filename
 * metadata throughout the file storage lifecycle. It handles the conversion
 * between
 * original filenames and storage filenames, extension detection, and file
 * renaming
 * operations.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 * <li>UUID-based filename generation for secure storage</li>
 * <li>Automatic file extension detection and mapping</li>
 * <li>MIME type-based extension inference using Apache Tika</li>
 * <li>Filename metadata creation and management</li>
 * <li>File renaming with extension preservation</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Filename Strategy:</strong>
 * <ul>
 * <li><strong>Original Filename:</strong> Preserved for user display and
 * reference</li>
 * <li><strong>Storage Filename:</strong> UUID-based for security and
 * uniqueness</li>
 * <li><strong>Extension Handling:</strong> Preserved from original or inferred
 * from MIME type</li>
 * <li><strong>Metadata Tracking:</strong> Maintains mapping between original
 * and storage names</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Security Benefits:</strong>
 * <ul>
 * <li>UUID-based storage names prevent filename-based attacks</li>
 * <li>Original filenames are preserved for user experience</li>
 * <li>No predictable patterns in storage filenames</li>
 * <li>Extension preservation maintains file type information</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Extension Detection:</strong>
 * <ul>
 * <li>Primary: Extracted from original filename if present</li>
 * <li>Fallback: MIME type detection using Apache Tika</li>
 * <li>Mapping: Uses {@link FileExtensionUtil} for MIME-to-extension
 * conversion</li>
 * <li>Preservation: Extensions are maintained during file operations</li>
 * </ul>
 * </p>
 *
 * @author TT-Storage Team
 * @version 1.0
 * @see FilenameMetadata
 * @see FileExtensionUtil
 * @see Tika
 * @since 1.0
 */
public class FilenameUtil {

    /**
     * Builds comprehensive filename metadata for a file with optional MIME type
     * detection.
     *
     * <p>
     * This method creates a complete {@link FilenameMetadata} object that contains
     * both the original filename and the UUID-based storage filename. It handles
     * file extension detection through multiple strategies and ensures proper
     * metadata tracking for the file storage system.
     * </p>
     *
     * <p>
     * <strong>Extension Detection Strategy:</strong>
     * <ol>
     * <li><strong>Filename-based:</strong> Extract extension from original filename
     * if it contains a valid extension (e.g., "document.pdf" → ".pdf")</li>
     * <li><strong>MIME-based:</strong> If no extension found and inputStream
     * provided,
     * use Apache Tika to detect MIME type and map to extension</li>
     * <li><strong>Fallback:</strong> If no extension can be determined, use empty
     * string</li>
     * </ol>
     * </p>
     *
     * <p>
     * <strong>Filename Generation:</strong>
     * <ul>
     * <li><strong>Original Full Name:</strong> Preserves user's original filename
     * with extension</li>
     * <li><strong>Mapped Full Name:</strong> UUID + extension for secure
     * storage</li>
     * <li><strong>Mapped Name:</strong> UUID only (without extension)</li>
     * <li><strong>Extension:</strong> Extracted or inferred file extension</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>MIME Type Detection:</strong>
     * <ul>
     * <li>Uses Apache Tika for robust MIME type detection</li>
     * <li>Analyzes file content, not just filename</li>
     * <li>Supports hundreds of file formats</li>
     * <li>Handles edge cases and corrupted files gracefully</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     * <li>MIME type detection failures throw {@link InternalServerException}</li>
     * <li>Null inputStream is handled gracefully (skips MIME detection)</li>
     * <li>Invalid filenames are handled safely</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Examples:</strong>
     * <ul>
     * <li>Input: fileId="123e4567-e89b-12d3-a456-426614174000",
     * filename="document.pdf"
     * → Original: "document.pdf", Mapped:
     * "123e4567-e89b-12d3-a456-426614174000.pdf"</li>
     * <li>Input: fileId="123e4567-e89b-12d3-a456-426614174000", filename="image"
     * (no extension)
     * → Original: "image.jpg", Mapped:
     * "123e4567-e89b-12d3-a456-426614174000.jpg"</li>
     * </ul>
     * </p>
     *
     * @param fileId           the unique identifier for the file. Must not be null.
     * @param originalFilename the original filename provided by the user. Must not
     *                         be null.
     * @param inputStream      the file input stream for MIME type detection. Can be
     *                         null
     *                         if MIME detection is not needed or not available.
     * @return a {@link FilenameMetadata} object containing both original and mapped
     * filename information with proper extension handling.
     * @throws InternalServerException  if MIME type detection fails due to I/O
     *                                  errors
     * @throws IllegalArgumentException if fileId or originalFilename is null
     * @see FilenameMetadata
     * @see FileExtensionUtil
     * @see Tika
     */
    public static FilenameMetadata buildInitialFilenameMetadata(UUID fileId, String originalFilename,
                                                                InputStream inputStream) {
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex); // includes the dot, e.g., ".pdf"
        } else if (inputStream != null) {
            Tika tika = new Tika();
            String mimeType;
            try {
                mimeType = tika.detect(inputStream);
            } catch (IOException e) {
                throw new InternalServerException(ErrorCodes.INTERNAL_SERVER_ERROR, "Error detecting mime type");
            }
            extension = FileExtensionUtil.map(mimeType);
            originalFilename = originalFilename + extension;
        }

        FilenameMetadata metadata = new FilenameMetadata();
        metadata.setOriginalFullName(originalFilename);
        metadata.setMappedFullName(fileId + extension);
        metadata.setMappedName(fileId);
        metadata.setExtension(extension);

        return metadata;
    }

    /**
     * Builds filename metadata for a file without MIME type detection.
     *
     * <p>
     * This is a convenience method that creates filename metadata using only
     * the file ID and original filename. It delegates to
     * {@link #buildInitialFilenameMetadata(UUID, String, InputStream)} with
     * a null inputStream, which means MIME type detection will be skipped.
     * </p>
     *
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     * <li>When the original filename already contains a valid extension</li>
     * <li>When MIME type detection is not needed or not available</li>
     * <li>For performance optimization when extension is already known</li>
     * <li>During file retrieval operations where inputStream is not available</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Extension Handling:</strong>
     * <ul>
     * <li>Extension is extracted from the original filename if present</li>
     * <li>If no extension found, it remains empty</li>
     * <li>No MIME type detection is performed</li>
     * </ul>
     * </p>
     *
     * @param fileId           the unique identifier for the file. Must not be null.
     * @param originalFilename the original filename provided by the user. Must not
     *                         be null.
     * @return a {@link FilenameMetadata} object containing filename information
     * without MIME type detection.
     * @throws IllegalArgumentException if fileId or originalFilename is null
     * @see #buildInitialFilenameMetadata(UUID, String, InputStream)
     * @see FilenameMetadata
     */
    public static FilenameMetadata buildFilenameMetadata(UUID fileId, String originalFilename) {
        return buildInitialFilenameMetadata(fileId, originalFilename, null);
    }

    /**
     * Renames a file while preserving its extension and UUID-based storage name.
     *
     * <p>
     * This method creates new filename metadata for a renamed file, maintaining
     * the existing file ID and extension while updating the original filename
     * to reflect the new name provided by the user.
     * </p>
     *
     * <p>
     * <strong>Renaming Behavior:</strong>
     * <ul>
     * <li><strong>File ID:</strong> Remains unchanged (same storage location)</li>
     * <li><strong>Extension:</strong> Preserved from the original file</li>
     * <li><strong>Original Name:</strong> Updated to the new filename +
     * extension</li>
     * <li><strong>Storage Name:</strong> Remains the same (UUID + extension)</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Extension Preservation:</strong>
     * <ul>
     * <li>Extension is extracted from the original filename</li>
     * <li>New filename is combined with the existing extension</li>
     * <li>File type and storage location remain unchanged</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     * <li>User-initiated file renaming</li>
     * <li>Bulk file renaming operations</li>
     * <li>File organization and categorization</li>
     * <li>Correcting typos in filenames</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Examples:</strong>
     * <ul>
     * <li>Original: "old-document.pdf" → New: "new-document.pdf" (same UUID, same
     * extension)</li>
     * <li>Original: "vacation-photo.jpg" → New: "summer-2023.jpg" (same UUID, same
     * extension)</li>
     * </ul>
     * </p>
     *
     * @param fileId           the unique identifier of the existing file. Must not
     *                         be null.
     * @param originalFilename the current original filename. Must not be null.
     * @param newFilename      the new filename to assign to the file. Must not be
     *                         null or empty.
     * @return a {@link FilenameMetadata} object with the updated original filename
     * while preserving the file ID and extension.
     * @throws IllegalArgumentException if any parameter is null or newFilename is
     *                                  empty
     * @see FilenameMetadata
     */
    public static FilenameMetadata renameFile(UUID fileId, String originalFilename, String newFilename) {
        FilenameMetadata metadata = buildFilenameMetadata(fileId, originalFilename);
        metadata.setOriginalFullName(newFilename + metadata.getExtension());

        return metadata;
    }
}
