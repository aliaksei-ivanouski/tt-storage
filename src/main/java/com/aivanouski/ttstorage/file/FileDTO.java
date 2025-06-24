package com.aivanouski.ttstorage.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@Schema(name = "FileDTO")
public class FileDTO {
    private UUID id;
    private String filename;
    private UUID userId;
    private Set<String> tags;
    private long size;
    private File.Visibility visibility;
    private String contentType;
    private String downloadLink;
    private Instant createdAt;
    private Instant updatedAt;
}