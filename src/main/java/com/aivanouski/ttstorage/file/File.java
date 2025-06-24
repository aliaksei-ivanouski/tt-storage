package com.aivanouski.ttstorage.file;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Document(collection = "files")
@CompoundIndex(
        name = "unique_file_per_user_and_content",
        def = "{'userId': 1, 'md5': 1, 'filename': 1}",
        unique = true
)
@Data
public class File {

    @Id
    private String id;

    @Indexed(unique = true)
    private UUID fileId;

    private UUID userId;

    private String filename;

    private String md5;

    @Indexed
    private Set<String> tags;

    private long size;

    @Indexed
    private Visibility visibility;

    private String contentType;

    private Instant createdAt;

    private Instant updatedAt;

    public enum Visibility {
        PUBLIC,
        PRIVATE
    }
}