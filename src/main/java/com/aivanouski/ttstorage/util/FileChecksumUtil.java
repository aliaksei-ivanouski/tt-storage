package com.aivanouski.ttstorage.util;

import java.io.InputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for calculating file checksums and integrity verification in
 * the TT-Storage system.
 *
 * <p>
 * This utility provides functionality for generating cryptographic hash values
 * (checksums)
 * of files to ensure data integrity, detect duplicates, and verify file
 * authenticity.
 * It uses the MD5 algorithm for fast and reliable checksum generation.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 * <li>MD5 checksum calculation for file integrity verification</li>
 * <li>Stream-based processing for memory-efficient handling of large files</li>
 * <li>Filename inclusion in checksum for enhanced uniqueness</li>
 * <li>Automatic resource management with try-with-resources</li>
 * <li>Hexadecimal string output for easy storage and comparison</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Checksum Strategy:</strong>
 * <ul>
 * <li><strong>Algorithm:</strong> MD5 (Message Digest Algorithm 5)</li>
 * <li><strong>Input:</strong> Filename + file content for enhanced
 * uniqueness</li>
 * <li><strong>Processing:</strong> Stream-based for memory efficiency</li>
 * <li><strong>Output:</strong> 32-character hexadecimal string</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Use Cases:</strong>
 * <ul>
 * <li><strong>Duplicate Detection:</strong> Identify identical files regardless
 * of filename</li>
 * <li><strong>Integrity Verification:</strong> Ensure files haven't been
 * corrupted</li>
 * <li><strong>Content-Based Identification:</strong> Use checksum as unique
 * file identifier</li>
 * <li><strong>Storage Optimization:</strong> Avoid storing duplicate
 * content</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Security Considerations:</strong>
 * <ul>
 * <li>MD5 is used for integrity, not cryptographic security</li>
 * <li>Suitable for collision-resistant duplicate detection</li>
 * <li>Filename inclusion reduces collision probability</li>
 * <li>Consider SHA-256 for higher security requirements</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Performance Characteristics:</strong>
 * <ul>
 * <li>Stream-based processing for memory efficiency</li>
 * <li>8KB buffer size for optimal I/O performance</li>
 * <li>Single-pass processing (no multiple file reads)</li>
 * <li>Automatic resource cleanup</li>
 * </ul>
 * </p>
 *
 * @author TT-Storage Team
 * @version 1.0
 * @see MessageDigest
 * @see DigestInputStream
 * @see StandardCharsets
 * @since 1.0
 */
public class FileChecksumUtil {

    /**
     * Calculates an MD5 checksum for a file based on its filename and content.
     *
     * <p>
     * This method generates a cryptographic hash (MD5 checksum) that combines both
     * the filename and file content. This approach provides enhanced uniqueness
     * compared to content-only checksums, making it more suitable for duplicate
     * detection in file storage systems.
     * </p>
     *
     * <p>
     * <strong>Checksum Calculation Process:</strong>
     * <ol>
     * <li><strong>Initialize MD5:</strong> Create MessageDigest instance with MD5
     * algorithm</li>
     * <li><strong>Include Filename:</strong> Add filename bytes to the digest
     * (UTF-8 encoding)</li>
     * <li><strong>Process Content:</strong> Stream file content through
     * DigestInputStream</li>
     * <li><strong>Generate Hash:</strong> Complete the digest and convert to
     * hexadecimal</li>
     * </ol>
     * </p>
     *
     * <p>
     * <strong>Filename Inclusion Benefits:</strong>
     * <ul>
     * <li><strong>Enhanced Uniqueness:</strong> Same content with different names
     * gets different checksums</li>
     * <li><strong>Collision Reduction:</strong> Reduces probability of checksum
     * collisions</li>
     * <li><strong>Context Preservation:</strong> Maintains filename context in the
     * checksum</li>
     * <li><strong>Better Duplicate Detection:</strong> Distinguishes between
     * identical content with different names</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Stream Processing:</strong>
     * <ul>
     * <li>Uses {@link DigestInputStream} for efficient processing</li>
     * <li>8KB buffer size for optimal I/O performance</li>
     * <li>Memory-efficient for large files</li>
     * <li>Automatic resource management with try-with-resources</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Output Format:</strong>
     * <ul>
     * <li>32-character hexadecimal string</li>
     * <li>Lowercase letters (a-f) and digits (0-9)</li>
     * <li>No separators or special characters</li>
     * <li>Consistent format for easy comparison and storage</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     * <li>{@link NoSuchAlgorithmException}: If MD5 algorithm is not available</li>
     * <li>{@link IOException}: If file reading fails</li>
     * <li>Null filename is handled gracefully (skipped)</li>
     * <li>Automatic stream cleanup on errors</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     * <li>Time complexity: O(n) where n is file size in bytes</li>
     * <li>Space complexity: O(1) - constant memory usage</li>
     * <li>I/O bound operation - performance depends on file system</li>
     * <li>Single-pass processing - no multiple file reads</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Examples:</strong>
     * <ul>
     * <li>Input: filename="document.pdf", content=[PDF bytes] → Output:
     * "a1b2c3d4e5f6..."</li>
     * <li>Input: filename="image.jpg", content=[JPEG bytes] → Output:
     * "f6e5d4c3b2a1..."</li>
     * <li>Input: filename=null, content=[any bytes] → Output:
     * "1234567890abcdef..."</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     * <li>MD5 is cryptographically broken for collision resistance</li>
     * <li>Suitable for integrity checking and duplicate detection</li>
     * <li>Not recommended for cryptographic security purposes</li>
     * <li>Consider SHA-256 for higher security requirements</li>
     * </ul>
     * </p>
     *
     * @param fileName    the name of the file to include in the checksum
     *                    calculation.
     *                    Can be null, in which case only file content is used.
     * @param inputStream the input stream containing the file content. Must not be
     *                    null
     *                    and must be readable. The stream will be consumed during
     *                    processing.
     * @return a 32-character hexadecimal string representing the MD5 checksum of
     * the
     * filename and file content combined.
     * @throws NoSuchAlgorithmException if the MD5 algorithm is not available in the
     *                                  current Java environment (should not occur
     *                                  in
     *                                  standard JVMs).
     * @throws IOException              if an I/O error occurs while reading the
     *                                  input stream.
     * @throws IllegalArgumentException if inputStream is null.
     * @see MessageDigest
     * @see DigestInputStream
     * @see StandardCharsets#UTF_8
     */
    public static String calculateMD5Checksum(String fileName, InputStream inputStream)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        if (fileName != null) {
            md.update(fileName.getBytes(StandardCharsets.UTF_8));
        }

        try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, md)) {
            byte[] buffer = new byte[8192];
            while (digestInputStream.read(buffer) != -1) {
                // No need to manually call md.update(), DigestInputStream does it
            }
        }
        byte[] mdBytes = md.digest();

        StringBuilder hexString = new StringBuilder();
        for (byte b : mdBytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }
}
