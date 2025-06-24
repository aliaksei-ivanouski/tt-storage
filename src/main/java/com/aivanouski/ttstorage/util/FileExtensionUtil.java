package com.aivanouski.ttstorage.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for mapping MIME types to file extensions in the TT-Storage
 * system.
 *
 * <p>
 * This utility provides functionality for converting MIME types to appropriate
 * file
 * extensions, enabling proper file type identification and handling when
 * original
 * filenames lack extensions. It maintains a comprehensive mapping of common
 * MIME
 * types to their corresponding file extensions.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 * <li>MIME type to file extension mapping for common file formats</li>
 * <li>Static initialization for optimal performance</li>
 * <li>Fallback handling for unknown MIME types</li>
 * <li>Integration with filename processing utilities</li>
 * <li>Support for popular document, image, and archive formats</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Supported File Types:</strong>
 * <ul>
 * <li><strong>Images:</strong> JPEG, PNG</li>
 * <li><strong>Documents:</strong> PDF, TXT, DOC, DOCX</li>
 * <li><strong>Web:</strong> HTML, XML, JSON</li>
 * <li><strong>Archives:</strong> ZIP</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Mapping Strategy:</strong>
 * <ul>
 * <li><strong>Static Initialization:</strong> Mappings loaded once at class
 * initialization</li>
 * <li><strong>Case-Sensitive:</strong> MIME types must match exactly</li>
 * <li><strong>Fallback Value:</strong> Unknown MIME types return "unknown"
 * extension</li>
 * <li><strong>Dot Prefix:</strong> Extensions include the leading dot (e.g.,
 * ".pdf")</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Use Cases:</strong>
 * <ul>
 * <li>File extension inference from MIME type detection</li>
 * <li>Filename completion when original names lack extensions</li>
 * <li>File type validation and verification</li>
 * <li>Integration with Apache Tika MIME detection</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Performance Characteristics:</strong>
 * <ul>
 * <li>O(1) lookup time using HashMap</li>
 * <li>Static initialization - no runtime overhead</li>
 * <li>Memory efficient with small, focused mapping</li>
 * <li>Thread-safe for concurrent access</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Integration Points:</strong>
 * <ul>
 * <li>Used by {@link FilenameUtil} for extension detection</li>
 * <li>Works with Apache Tika MIME type detection</li>
 * <li>Supports file upload and processing workflows</li>
 * <li>Enables proper file type handling in storage system</li>
 * </ul>
 * </p>
 *
 * @author TT-Storage Team
 * @version 1.0
 * @see FilenameUtil
 * @since 1.0
 */
public class FileExtensionUtil {

    /**
     * Static mapping of MIME types to their corresponding file extensions.
     *
     * <p>
     * This map contains the most commonly used MIME types and their associated
     * file extensions. The mappings are initialized once at class loading time
     * for optimal performance. Extensions include the leading dot for proper
     * filename construction.
     * </p>
     *
     * <p>
     * <strong>Supported Mappings:</strong>
     * <ul>
     * <li>image/jpeg → .jpg</li>
     * <li>image/png → .png</li>
     * <li>application/pdf → .pdf</li>
     * <li>text/plain → .txt</li>
     * <li>application/msword → .doc</li>
     * <li>application/vnd.openxmlformats-officedocument.wordprocessingml.document →
     * .docx</li>
     * <li>application/zip → .zip</li>
     * <li>application/json → .json</li>
     * <li>text/html → .html</li>
     * <li>application/xml → .xml</li>
     * </ul>
     * </p>
     */
    private static final Map<String, String> mimeToExtensionMap = new HashMap<>();

    /**
     * Static initializer block that populates the MIME type to extension mapping.
     *
     * <p>
     * This block is executed once when the class is loaded, ensuring that all
     * mappings are available immediately without any runtime initialization
     * overhead.
     * The mappings cover the most common file types encountered in file storage
     * systems.
     * </p>
     */
    static {
        mimeToExtensionMap.put("image/jpeg", ".jpg");
        mimeToExtensionMap.put("image/png", ".png");
        mimeToExtensionMap.put("application/pdf", ".pdf");
        mimeToExtensionMap.put("text/plain", ".txt");
        mimeToExtensionMap.put("application/msword", ".doc");
        mimeToExtensionMap.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");
        mimeToExtensionMap.put("application/zip", ".zip");
        mimeToExtensionMap.put("application/json", ".json");
        mimeToExtensionMap.put("text/html", ".html");
        mimeToExtensionMap.put("application/xml", ".xml");
    }

    /**
     * Maps a MIME type to its corresponding file extension.
     *
     * <p>
     * This method performs a lookup in the static MIME type mapping to find the
     * appropriate file extension for a given MIME type. It provides a fallback
     * value for unknown MIME types to ensure robust handling of all file types.
     * </p>
     *
     * <p>
     * <strong>Lookup Process:</strong>
     * <ol>
     * <li><strong>Exact Match:</strong> Search for exact MIME type match in the
     * mapping</li>
     * <li><strong>Extension Return:</strong> Return the corresponding extension if
     * found</li>
     * <li><strong>Fallback:</strong> Return "unknown" if MIME type is not in the
     * mapping</li>
     * </ol>
     * </p>
     *
     * <p>
     * <strong>Extension Format:</strong>
     * <ul>
     * <li>All extensions include the leading dot (e.g., ".pdf", ".jpg")</li>
     * <li>Consistent format for easy filename construction</li>
     * <li>Lowercase extensions for standardization</li>
     * <li>Fallback value is "unknown" (without dot)</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Performance Characteristics:</strong>
     * <ul>
     * <li>O(1) average case lookup time using HashMap</li>
     * <li>No memory allocation for successful lookups</li>
     * <li>Thread-safe for concurrent access</li>
     * <li>Minimal CPU overhead</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     * <li>Null MIME type returns "unknown"</li>
     * <li>Empty string returns "unknown"</li>
     * <li>Unknown MIME types return "unknown"</li>
     * <li>No exceptions thrown for any input</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     * <li><strong>File Upload:</strong> Infer extension when filename lacks
     * one</li>
     * <li><strong>MIME Detection:</strong> Convert detected MIME type to
     * extension</li>
     * <li><strong>Filename Construction:</strong> Build proper filenames with
     * extensions</li>
     * <li><strong>File Type Validation:</strong> Verify expected file types</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Examples:</strong>
     * <ul>
     * <li>Input: "image/jpeg" → Output: ".jpg"</li>
     * <li>Input: "application/pdf" → Output: ".pdf"</li>
     * <li>Input: "text/plain" → Output: ".txt"</li>
     * <li>Input: "unknown/mime" → Output: "unknown"</li>
     * <li>Input: null → Output: "unknown"</li>
     * <li>Input: "" → Output: "unknown"</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Integration Notes:</strong>
     * <ul>
     * <li>Designed to work with Apache Tika MIME detection</li>
     * <li>Used by {@link FilenameUtil#buildInitialFilenameMetadata}</li>
     * <li>Supports file upload workflows</li>
     * <li>Enables proper file type handling in storage</li>
     * </ul>
     * </p>
     *
     * @param mimeType the MIME type to map to a file extension. Can be null or
     *                 empty,
     *                 in which case "unknown" will be returned. The lookup is
     *                 case-sensitive and requires exact matches.
     * @return the corresponding file extension including the leading dot (e.g.,
     * ".pdf"),
     * or "unknown" if the MIME type is not found in the mapping or if the
     * input is null/empty.
     * @see HashMap#getOrDefault(Object, Object)
     */
    public static String map(String mimeType) {
        return mimeToExtensionMap.getOrDefault(mimeType, "unknown");
    }
}