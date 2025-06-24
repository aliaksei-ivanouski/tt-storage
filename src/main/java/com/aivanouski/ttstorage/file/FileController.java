package com.aivanouski.ttstorage.file;

import com.aivanouski.ttstorage.global.apidocs.FileIdDoc;
import com.aivanouski.ttstorage.global.apidocs.PageableDoc;
import com.aivanouski.ttstorage.global.apidocs.TagsDoc;
import com.aivanouski.ttstorage.global.apidocs.UserIdDoc;
import com.aivanouski.ttstorage.storage.TTFile;
import com.aivanouski.ttstorage.tag.TagService;
import com.aivanouski.ttstorage.validation.ValidUUID;
import com.aivanouski.ttstorage.validation.ValidVisibility;
import com.aivanouski.ttstorage.web.Success;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File operations")
@Validated
public class FileController {

    private final FileService fileService;
    private final TagService tagService;

    public FileController(FileService fileService, TagService tagService) {
        this.fileService = fileService;
        this.tagService = tagService;
    }

    @Operation(summary = "Upload file", description = "Upload the file to the storage.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileDTO uploadFile(
            @RequestParam("file") MultipartFile file,
            @UserIdDoc @RequestParam("userId") @ValidUUID String userId,
            @RequestParam("visibility") @ValidVisibility String visibility,
            @Size(max = 5, message = "maximum 5 tags allowed per file")
            @TagsDoc @RequestParam(value = "tags", required = false) Set<String> tags) {
        FileDTO dto = fileService.uploadFile(
                UUID.fromString(userId),
                file.getContentType(),
                File.Visibility.valueOf(visibility),
                tags != null ? tags : new HashSet<>(),
                file);
        tagService.register(dto.getTags());
        log.info("Uploading the file with name '{}' to storage. User id: '{}'", file.getOriginalFilename(),
                userId);

        return dto;
    }

    @Operation(summary = "Download file", description = "Download the file by the direct link.")
    @GetMapping("/{fileId}/users/{userId}")
    public ResponseEntity<InputStreamResource> getFile(
            @FileIdDoc @ValidUUID @PathVariable String fileId,
            @UserIdDoc @ValidUUID @PathVariable String userId) {
        TTFile file = fileService.getFile(UUID.fromString(userId), UUID.fromString(fileId));
        log.info("Retrieving the file with fileId: '{}' from storage by userId: '{}'", fileId, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));
        String filename = file.getMetadata().getFilename();
        headers.setContentDispositionFormData("attachment",
                new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1)
                        .intern());

        InputStreamResource fileResource = new InputStreamResource(file.getInputStream());

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileResource);
    }

    @Operation(summary = "List public files", description = "Get a paginated list of all public files saved in the storage.")
    @GetMapping("/public")
    public Page<FileDTO> listPublicFiles(
            @TagsDoc @RequestParam(defaultValue = "") Set<String> tags,
            @PageableDoc Pageable pageable) {
        Page<FileDTO> page = fileService.listPublicFiles(tags, pageable);
        log.info("Retrieving all files from the storage, total files: {}", page.getTotalElements());

        return page;
    }

    @Operation(summary = "List users' files", description = "Get a paginated list of all users' files saved in the storage.")
    @GetMapping("/users/{userId}")
    public Page<FileDTO> listUserFiles(
            @UserIdDoc @ValidUUID @PathVariable String userId,
            @TagsDoc @RequestParam(defaultValue = "") Set<String> tags,
            @PageableDoc Pageable pageable) {
        Page<FileDTO> page = fileService.listUserFiles(UUID.fromString(userId), tags, pageable);
        log.info("Retrieving all user's files from the storage, total files: {}", page.getTotalElements());

        return page;
    }

    @Operation(summary = "Rename file", description = "Rename the file stored in the storage.")
    @PutMapping(value = "/{fileId}/rename")
    public FileDTO renameFile(
            @FileIdDoc @ValidUUID @PathVariable String fileId,
            @Valid @RequestBody RenameFilePayload payload) {
        FileDTO dto = fileService.renameFile(
                UUID.fromString(payload.userId),
                UUID.fromString(fileId),
                payload.newFilename);
        log.info("Rename file with id {} to a new name: {}, by user with id: {}",
                fileId, payload.newFilename, payload.userId);

        return dto;
    }

    @Operation(summary = "Delete file", description = "Delete the file from the storage and its metadata from the storage.")
    @DeleteMapping("/{fileId}/users/{userId}")
    public Success deleteFile(
            @FileIdDoc @ValidUUID @PathVariable String fileId,
            @UserIdDoc @ValidUUID @PathVariable String userId) {
        fileService.deleteFile(UUID.fromString(userId), UUID.fromString(fileId));
        log.info("Deleting the file with fileId: '{}' from storage by userId: '{}'", fileId, userId);

        return new Success(true);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @Schema(name = "RenameFilePayload")
    public static class RenameFilePayload {
        @Size(min = 1, max = 50, message = "file name must not be empty or exceed 50 symbols")
        private String newFilename;
        @ValidUUID
        private String userId;
    }
}
