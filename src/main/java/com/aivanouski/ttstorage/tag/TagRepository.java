package com.aivanouski.ttstorage.tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing tag metadata in the TT-Storage system.
 *
 * <p>
 * This repository provides data access operations for tag metadata stored in
 * MongoDB.
 * It extends {@link MongoRepository} to leverage Spring Data MongoDB's powerful
 * query capabilities and custom MongoDB queries for advanced tag searching.
 * </p>
 *
 * <p>
 * The repository handles the following key operations:
 * <ul>
 * <li>Tag search with case-insensitive pattern matching</li>
 * <li>Pagination support for large tag collections</li>
 * <li>Automatic tag discovery and suggestion</li>
 * <li>Tag-based content organization</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Entity Type:</strong> {@link Tag} (String ID type for MongoDB
 * ObjectId)
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 * <li>Case-insensitive tag search using MongoDB regex queries</li>
 * <li>Custom MongoDB queries using {@code @Query} annotations</li>
 * <li>Pagination support via {@link Pageable} parameter</li>
 * <li>Pattern-based tag discovery for autocomplete functionality</li>
 * <li>Integration with file tagging system</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Tag System Overview:</strong>
 * <ul>
 * <li>Tags are used to categorize and organize files in the storage system</li>
 * <li>Tags support case-insensitive search and matching</li>
 * <li>Tags can be shared across multiple files and users</li>
 * <li>Tag discovery enables content organization and discovery</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Search Capabilities:</strong>
 * <ul>
 * <li>Partial tag matching (e.g., "photo" matches "photography", "photos")</li>
 * <li>Case-insensitive search (e.g., "Photo" matches "photo", "PHOTO")</li>
 * <li>Regex-based pattern matching for flexible search</li>
 * <li>Pagination for handling large tag collections</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Use Cases:</strong>
 * <ul>
 * <li>Tag autocomplete in user interfaces</li>
 * <li>Tag discovery for content organization</li>
 * <li>Tag-based file filtering and search</li>
 * <li>Tag analytics and usage statistics</li>
 * </ul>
 * </p>
 *
 * @author TT-Storage Team
 * @version 1.0
 * @see Tag
 * @see MongoRepository
 * @see Pageable
 * @see Page
 * @since 1.0
 */
@Repository
public interface TagRepository extends MongoRepository<Tag, String> {

    /**
     * Searches for tags that contain the specified search string, ignoring case.
     *
     * <p>
     * This method performs a case-insensitive pattern search on tag names using
     * MongoDB's regex capabilities. It enables partial matching, allowing users
     * to find tags by typing only a portion of the tag name.
     * </p>
     *
     * <p>
     * <strong>Query:</strong> Custom MongoDB query using {@code @Query} annotation:
     * <code>{'tagName': {'$regex': ?0, '$options': 'i'}}</code>
     * </p>
     *
     * <p>
     * <strong>Search Behavior:</strong>
     * <ul>
     * <li><strong>Case-insensitive:</strong> Search is performed regardless of case
     * (e.g., "photo" matches "Photo", "PHOTO", "photo")</li>
     * <li><strong>Partial matching:</strong> Search string can be a substring of
     * the tag
     * (e.g., "photo" matches "photography", "photos", "photo-album")</li>
     * <li><strong>Pattern-based:</strong> Uses MongoDB regex for flexible pattern
     * matching</li>
     * <li><strong>Empty search:</strong> If search string is null or empty, all
     * tags
     * will be returned (subject to pagination)</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     * <li>Regex queries can be slower than exact matches</li>
     * <li>Consider adding a text index on the tagName field for better
     * performance</li>
     * <li>Pagination helps manage large result sets efficiently</li>
     * <li>Search performance depends on the size of the tag collection</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     * <li><strong>Autocomplete:</strong> Provide tag suggestions as users type</li>
     * <li><strong>Tag discovery:</strong> Help users find existing tags</li>
     * <li><strong>Content organization:</strong> Suggest tags for file
     * categorization</li>
     * <li><strong>Search refinement:</strong> Filter tags for advanced search</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Examples:</strong>
     * <ul>
     * <li>Search "photo" → matches: "photo", "photography", "photos",
     * "photo-album"</li>
     * <li>Search "doc" → matches: "document", "docs", "documentation"</li>
     * <li>Search "vac" → matches: "vacation", "vacation-photos",
     * "vacation-2023"</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Security:</strong> This method is read-only and does not expose
     * sensitive information. Tag names are considered public metadata.
     * </p>
     *
     * @param search   the search string to match against tag names. Can be null or
     *                 empty,
     *                 in which case all tags will be returned (subject to
     *                 pagination).
     *                 The search is case-insensitive and supports partial matching.
     * @param pageable pagination and sorting criteria. If null, default pagination
     *                 (first 20 results) will be applied. Sorting can be applied to
     *                 tagName, usage count, or creation date.
     * @return a {@link Page} containing matching tags, sorted according to the
     * pageable
     * criteria. Returns empty page if no matching tags found.
     * @see Page
     * @see Pageable
     * @see Tag
     */
    @Query("{'tagName': {'$regex': ?0, '$options': 'i'}}")
    Page<Tag> findAllBySearch(String search, Pageable pageable);
}
