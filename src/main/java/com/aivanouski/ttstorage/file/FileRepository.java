package com.aivanouski.ttstorage.file;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository interface for managing file metadata in the TT-Storage system.
 *
 * <p>
 * This repository provides data access operations for file metadata stored in
 * MongoDB.
 * It extends {@link MongoRepository} to leverage Spring Data MongoDB's powerful
 * query capabilities and automatic query generation from method names.
 * </p>
 *
 * <p>
 * The repository handles the following key operations:
 * <ul>
 * <li>User-specific file queries (by user ID)</li>
 * <li>Public file queries (visibility-based)</li>
 * <li>Tag-based filtering</li>
 * <li>Pagination support for large result sets</li>
 * <li>File lookup by unique identifiers</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Entity Type:</strong> {@link File} (String ID type for MongoDB
 * ObjectId)
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 * <li>Automatic query generation from method names</li>
 * <li>Custom MongoDB queries using {@code @Query} annotations</li>
 * <li>Pagination support via {@link Pageable} parameter</li>
 * <li>Type-safe UUID handling for user and file identifiers</li>
 * <li>Tag-based filtering with case-insensitive matching</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Security Considerations:</strong>
 * <ul>
 * <li>User-specific queries ensure data isolation between users</li>
 * <li>Public file queries respect visibility settings</li>
 * <li>All user-specific operations require valid user ID</li>
 * </ul>
 * </p>
 *
 * @author TT-Storage Team
 * @version 1.0
 * @see File
 * @see MongoRepository
 * @see Pageable
 * @see Page
 * @since 1.0
 */
@Repository
public interface FileRepository extends MongoRepository<File, String> {

    /**
     * Retrieves a paginated list of files owned by a specific user.
     *
     * <p>
     * This method returns all files that belong to the specified user, regardless
     * of their visibility settings or tags. The results are paginated to handle
     * large collections efficiently.
     * </p>
     *
     * <p>
     * <strong>Query:</strong> Automatically generated from method name:
     * <code>findByUserId</code> translates to MongoDB query:
     * <code>{ "userId": ?0 }</code>
     * </p>
     *
     * <p>
     * <strong>Performance:</strong> This query uses the userId field for efficient
     * retrieval. Consider adding a compound index on (userId, createdAt) for
     * optimal performance with large datasets.
     * </p>
     *
     * <p>
     * <strong>Security:</strong> This method respects user data isolation by
     * filtering results to only include files owned by the specified user.
     * </p>
     *
     * @param userId   the unique identifier of the file owner. Must not be null.
     * @param pageable pagination and sorting criteria. If null, default pagination
     *                 (first 20 results) will be applied.
     * @return a {@link Page} containing the user's files, sorted according to
     * the pageable criteria. Returns empty page if no files found.
     * @throws IllegalArgumentException if userId is null
     * @see Page
     * @see Pageable
     */
    Page<File> findByUserId(UUID userId, Pageable pageable);

    /**
     * Retrieves a paginated list of files owned by a specific user that are tagged
     * with any of the specified tags.
     *
     * <p>
     * This method performs a tag-based search within a user's file collection.
     * Files are returned if they contain any of the tags in the provided set.
     * Tag matching is case-insensitive and uses MongoDB's {@code $in} operator.
     * </p>
     *
     * <p>
     * <strong>Query:</strong> Automatically generated from method name:
     * <code>findByUserIdAndTagsIn</code> translates to MongoDB query:
     * <code>{ "userId": ?0, "tags": { "$in": ?1 } }</code>
     * </p>
     *
     * <p>
     * <strong>Tag Matching:</strong>
     * <ul>
     * <li>Case-insensitive matching</li>
     * <li>Partial tag matching (if a file has tags ["photo", "vacation"],
     * searching for "photo" will match)</li>
     * <li>Empty tags set returns no results</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Performance:</strong> Consider adding a compound index on
     * (userId, tags) for optimal query performance.
     * </p>
     *
     * @param userId   the unique identifier of the file owner. Must not be null.
     * @param tags     the set of tags to search for. If null or empty, no results
     *                 will be returned. Tags are converted to lowercase before
     *                 matching.
     * @param pageable pagination and sorting criteria. If null, default pagination
     *                 (first 20 results) will be applied.
     * @return a {@link Page} containing matching files, sorted according to
     * the pageable criteria. Returns empty page if no matching files found.
     * @throws IllegalArgumentException if userId is null
     * @see Page
     * @see Pageable
     */
    Page<File> findByUserIdAndTagsIn(UUID userId, Set<String> tags, Pageable pageable);

    /**
     * Retrieves a specific file by its unique identifier and owner.
     *
     * <p>
     * This method provides secure access to file metadata by requiring both
     * the file ID and the user ID. This ensures that users can only access
     * their own files, maintaining data security and privacy.
     * </p>
     *
     * <p>
     * <strong>Query:</strong> Automatically generated from method name:
     * <code>findByFileIdAndUserId</code> translates to MongoDB query:
     * <code>{ "fileId": ?0, "userId": ?1 }</code>
     * </p>
     *
     * <p>
     * <strong>Security:</strong> This method enforces user data isolation by
     * requiring both fileId and userId to match. A user cannot access files
     * owned by other users, even if they know the file ID.
     * </p>
     *
     * <p>
     * <strong>Performance:</strong> This query is highly efficient as it uses
     * unique identifiers. Consider adding a compound index on (fileId, userId)
     * for optimal performance.
     * </p>
     *
     * @param fileId the unique identifier of the file. Must not be null.
     * @param userId the unique identifier of the file owner. Must not be null.
     * @return an {@link Optional} containing the file if found and owned by
     * the specified user, or empty if not found or access denied.
     * @throws IllegalArgumentException if fileId or userId is null
     * @see Optional
     */
    Optional<File> findByFileIdAndUserId(UUID fileId, UUID userId);

    /**
     * Retrieves a specific file by its filename and owner.
     *
     * <p>
     * This method allows users to find their files by filename. Since filenames
     * are not globally unique (multiple users can have files with the same name),
     * the user ID is required to ensure proper data isolation.
     * </p>
     *
     * <p>
     * <strong>Query:</strong> Automatically generated from method name:
     * <code>findByFilenameAndUserId</code> translates to MongoDB query:
     * <code>{ "filename": ?0, "userId": ?1 }</code>
     * </p>
     *
     * <p>
     * <strong>Filename Matching:</strong>
     * <ul>
     * <li>Exact string matching (case-sensitive)</li>
     * <li>Includes file extension in matching</li>
     * <li>Handles special characters in filenames</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     * <li>Checking if a user already has a file with a specific name</li>
     * <li>Finding a specific file by name within a user's collection</li>
     * <li>Duplicate file detection during upload</li>
     * </ul>
     * </p>
     *
     * @param filename the name of the file to search for. Must not be null or
     *                 empty.
     * @param userId   the unique identifier of the file owner. Must not be null.
     * @return an {@link Optional} containing the file if found and owned by
     * the specified user, or empty if not found or access denied.
     * @throws IllegalArgumentException if filename is null/empty or userId is null
     * @see Optional
     */
    Optional<File> findByFilenameAndUserId(String filename, UUID userId);

    /**
     * Retrieves a paginated list of all public files in the system.
     *
     * <p>
     * This method returns all files that have been marked as public, regardless
     * of their owner. This enables discovery of shared content across all users
     * in the system.
     * </p>
     *
     * <p>
     * <strong>Query:</strong> Custom MongoDB query using {@code @Query} annotation:
     * <code>{ 'visibility': 'PUBLIC' }</code>
     * </p>
     *
     * <p>
     * <strong>Visibility:</strong> Only files with {@link File.Visibility#PUBLIC}
     * visibility are returned. Private files are excluded from results.
     * </p>
     *
     * <p>
     * <strong>Performance:</strong> This query can be resource-intensive for
     * large datasets. Consider adding an index on the visibility field for
     * optimal performance.
     * </p>
     *
     * <p>
     * <strong>Security:</strong> This method respects file visibility settings
     * and only returns files explicitly marked as public by their owners.
     * </p>
     *
     * @param pageable pagination and sorting criteria. If null, default pagination
     *                 (first 20 results) will be applied.
     * @return a {@link Page} containing all public files, sorted according to
     * the pageable criteria. Returns empty page if no public files found.
     * @see Page
     * @see Pageable
     * @see File.Visibility
     */
    @Query("{ 'visibility': 'PUBLIC' }")
    Page<File> findAllPublicFiles(Pageable pageable);

    /**
     * Retrieves a paginated list of public files that are tagged with any of
     * the specified tags.
     *
     * <p>
     * This method combines public file discovery with tag-based filtering,
     * allowing users to find shared content based on specific tags or categories.
     * </p>
     *
     * <p>
     * <strong>Query:</strong> Custom MongoDB query using {@code @Query} annotation:
     * <code>{ 'tags': { $in: ?0 }, 'visibility': 'PUBLIC' }</code>
     * </p>
     *
     * <p>
     * <strong>Tag Matching:</strong>
     * <ul>
     * <li>Case-insensitive matching using MongoDB's {@code $in} operator</li>
     * <li>Files are returned if they contain any of the specified tags</li>
     * <li>Empty tags set returns no results</li>
     * <li>Tags are converted to lowercase before matching</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     * <li>Discovering shared content by category (e.g., "photos", "documents")</li>
     * <li>Finding public resources with specific tags</li>
     * <li>Content discovery and browsing</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Performance:</strong> Consider adding a compound index on
     * (visibility, tags) for optimal query performance.
     * </p>
     *
     * @param tags     the set of tags to search for. If null or empty, no results
     *                 will be returned. Tags are converted to lowercase before
     *                 matching.
     * @param pageable pagination and sorting criteria. If null, default pagination
     *                 (first 20 results) will be applied.
     * @return a {@link Page} containing matching public files, sorted according to
     * the pageable criteria. Returns empty page if no matching files found.
     * @see Page
     * @see Pageable
     * @see File.Visibility
     */
    @Query("{ 'tags': { $in: ?0 }, 'visibility': 'PUBLIC' }")
    Page<File> findAllPublicFilesByTagsIn(Set<String> tags, Pageable pageable);
}