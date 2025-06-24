package com.aivanouski.ttstorage.file;

import org.springframework.stereotype.Component;

@Component
public class FileMapper {
    public FileDTO toDTO(File file, String domain) {
        return new FileDTO(
                file.getFileId(),
                file.getFilename(),
                file.getUserId(),
                file.getTags(),
                file.getSize(),
                file.getVisibility(),
                file.getContentType(),
                String.format("%s/api/v1/files/%s/users/%s", domain, file.getFileId(), file.getUserId()),
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }
}