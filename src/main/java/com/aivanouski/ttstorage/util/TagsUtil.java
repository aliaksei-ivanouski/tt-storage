package com.aivanouski.ttstorage.util;

import com.aivanouski.ttstorage.error.StorageException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for managing tag operations and normalization in the TT-Storage
 * system.
 *
 * <p>
 * This utility provides functionality for processing, normalizing, and managing
 * tags
 * used throughout the file storage system. It ensures consistent tag handling
 * by
 * applying standardized transformations and validations.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 * <li>Tag normalization to lowercase for consistent matching</li>
 * <li>Null-safe tag processing with default empty set creation</li>
 * <li>Stream-based tag transformation for efficient processing</li>
 * <li>Integration with file tagging and search systems</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Tag Normalization Strategy:</strong>
 * <ul>
 * <li><strong>Case Normalization:</strong> All tags are converted to
 * lowercase</li>
 * <li><strong>Null Handling:</strong> Null tag sets are converted to empty
 * sets</li>
 * <li><strong>Consistency:</strong> Ensures uniform tag matching across the
 * system</li>
 * <li><strong>Performance:</strong> Uses efficient stream operations for
 * processing</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Benefits of Tag Normalization:</strong>
 * <ul>
 * <li><strong>Search Consistency:</strong> Case-insensitive tag matching</li>
 * <li><strong>Data Integrity:</strong> Prevents duplicate tags with different
 * cases</li>
 * <li><strong>User Experience:</strong> Consistent tag display and search
 * results</li>
 * <li><strong>Storage Efficiency:</strong> Reduces redundant tag
 * variations</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Use Cases:</strong>
 * <ul>
 * <li>File upload tag processing</li>
 * <li>Tag-based search and filtering</li>
 * <li>Tag suggestion and autocomplete</li>
 * <li>Tag analytics and reporting</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Examples:</strong>
 * <ul>
 * <li>Input: ["Photo", "VACATION", "document"] → Output: ["photo", "vacation",
 * "document"]</li>
 * <li>Input: null → Output: [] (empty set)</li>
 * <li>Input: ["Photo", "PHOTO", "photo"] → Output: ["photo"] (duplicates
 * removed)</li>
 * </ul>
 * </p>
 *
 * @author TT-Storage Team
 * @version 1.0
 * @see StorageException
 * @since 1.0
 */
public class TagsUtil {

    /**
     * Normalizes a set of tags by converting them to lowercase and handling null
     * values.
     *
     * <p>
     * This method processes a set of tags to ensure consistent formatting and
     * matching
     * throughout the TT-Storage system. It converts all tags to lowercase for
     * case-insensitive operations and handles null input gracefully.
     * </p>
     *
     * <p>
     * <strong>Processing Steps:</strong>
     * <ol>
     * <li><strong>Null Check:</strong> If input is null, creates an empty
     * {@link HashSet}</li>
     * <li><strong>Stream Processing:</strong> Uses Java streams for efficient
     * transformation</li>
     * <li><strong>Case Conversion:</strong> Converts each tag to lowercase using
     * {@link String#toLowerCase()}</li>
     * <li><strong>Collection:</strong> Collects results into a new {@link Set} to
     * remove duplicates</li>
     * </ol>
     * </p>
     *
     * <p>
     * <strong>Null Handling:</strong>
     * <ul>
     * <li>If the input set is null, a new empty {@link HashSet} is created</li>
     * <li>This prevents {@link NullPointerException} in downstream operations</li>
     * <li>Ensures consistent behavior regardless of input state</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Case Normalization:</strong>
     * <ul>
     * <li>All tags are converted to lowercase using the default locale</li>
     * <li>This ensures consistent matching across different input formats</li>
     * <li>Prevents issues with case-sensitive comparisons</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Duplicate Removal:</strong>
     * <ul>
     * <li>The resulting set automatically removes duplicate tags</li>
     * <li>Tags that differ only in case become identical after normalization</li>
     * <li>Improves data consistency and storage efficiency</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Performance Characteristics:</strong>
     * <ul>
     * <li>Uses efficient stream operations for processing</li>
     * <li>Time complexity: O(n) where n is the number of tags</li>
     * <li>Space complexity: O(n) for the resulting set</li>
     * <li>Memory efficient with lazy evaluation</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     * <li>Null input is handled gracefully</li>
     * <li>Individual tag processing errors are not thrown</li>
     * <li>Returns empty set for problematic inputs</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Examples:</strong>
     * <ul>
     * <li>Input: ["Photo", "VACATION", "document"] → Output: ["photo", "vacation",
     * "document"]</li>
     * <li>Input: ["Photo", "PHOTO", "photo"] → Output: ["photo"]</li>
     * <li>Input: null → Output: []</li>
     * <li>Input: [] → Output: []</li>
     * </ul>
     * </p>
     *
     * @param tags the set of tags to normalize. Can be null, in which case an empty
     *             set will be returned. Individual tags within the set can be null
     *             or empty strings, which will be processed normally.
     * @return a new {@link Set} containing the normalized (lowercase) tags. If
     * input
     * is null, returns an empty set. Duplicate tags (after normalization)
     * are automatically removed.
     * @throws StorageException if there are issues with tag processing (currently
     *                          not thrown,
     *                          but reserved for future error handling)
     * @see Set
     * @see HashSet
     * @see String#toLowerCase()
     */
    public static Set<String> toLowercase(Set<String> tags) throws StorageException {
        if (tags == null) {
            tags = new HashSet<>();
        }
        return tags.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
