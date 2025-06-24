package com.aivanouski.ttstorage.file;

import lombok.Data;

import java.util.UUID;

@Data
public class FilenameMetadata {
    private String originalFullName;
    private String mappedFullName;
    private UUID mappedName;
    private String extension;
}
