package com.aivanouski.ttstorage.file;

import com.aivanouski.ttstorage.error.NotFoundException;
import com.aivanouski.ttstorage.error.StorageException;
import com.aivanouski.ttstorage.error.model.BadRequestException;
import com.aivanouski.ttstorage.storage.StorageService;
import com.aivanouski.ttstorage.storage.TTFile;
import com.aivanouski.ttstorage.util.FileChecksumUtil;
import com.aivanouski.ttstorage.util.FilenameUtil;
import com.aivanouski.ttstorage.util.TagsUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.aivanouski.ttstorage.error.model.ErrorCodes.DELETE_FILE_FROM_DB_ERROR;
import static com.aivanouski.ttstorage.error.model.ErrorCodes.DELETE_FILE_FROM_STORAGE_ERROR;
import static com.aivanouski.ttstorage.error.model.ErrorCodes.FILE_UPLOAD_ERROR;
import static com.aivanouski.ttstorage.error.model.ErrorCodes.NOT_FOUND_OR_ACCESS_DENIED_ERROR;
import static com.aivanouski.ttstorage.error.model.ErrorCodes.SAME_FILE_ERROR;

@Slf4j
@Service
public class DefaultFileService implements FileService {

    private final StorageService storageService;
    private final FileRepository fileRepository;
    private final FileMapper fileMapper;
    private final String domain;
    private final String tempDir;

    public DefaultFileService(StorageService storageService, FileRepository fileRepository, FileMapper fileMapper,
                              @Value("${app.domain}") String domain,
                              @Value("${spring.servlet.multipart.location:${java.io.tmpdir}}") String tempDir) {
        this.storageService = storageService;
        this.fileRepository = fileRepository;
        this.fileMapper = fileMapper;
        this.domain = domain;
        this.tempDir = tempDir;
    }

    @Override
    public FileDTO uploadFile(
            UUID userId,
            String contentType,
            File.Visibility visibility,
            Set<String> tags,
            MultipartFile multipartFile) {
        UUID fileId = UUID.randomUUID();
        String filename = multipartFile.getOriginalFilename();
        long size = multipartFile.getSize();

        Optional<File> existentFile = fileRepository.findByFilenameAndUserId(filename, userId);

        log.info("Starting streaming upload for large file: {} ({} bytes)", filename, size);

        try {
            Path tempDirPath = Path.of(tempDir);
            if (!Files.exists(tempDirPath)) {
                Files.createDirectories(tempDirPath);
            }
            Path tempFile = Files.createTempFile(tempDirPath, "tt-storage-upload-", ".tmp");

            try {
                try (InputStream inputStream = multipartFile.getInputStream()) {
                    Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                String md5;
                try (InputStream fileInputStream = Files.newInputStream(tempFile)) {
                    md5 = FileChecksumUtil.calculateMD5Checksum(filename, fileInputStream);
                }
                if (existentFile.isPresent() && existentFile.get().getMd5().equals(md5)) {
                    throw new BadRequestException(SAME_FILE_ERROR, "The file already exists");
                }

                if (contentType == null || contentType.isEmpty()) {
                    contentType = new Tika().detect(tempFile.toFile());
                }

                storageService.uploadFile(multipartFile, fileId);
                log.info("Successfully uploaded large file to storage: {} ({} bytes)", filename, size);

                FilenameMetadata filenameMetadata = FilenameUtil.buildFilenameMetadata(fileId, filename);

                File file = new File();
                file.setFileId(filenameMetadata.getMappedName());
                file.setUserId(userId);
                file.setFilename(filenameMetadata.getOriginalFullName());
                file.setMd5(md5);
                file.setTags(tags);
                file.setSize(size);
                file.setVisibility(visibility);
                file.setContentType(contentType);
                file.setCreatedAt(Instant.now());
                file.setUpdatedAt(Instant.now());

                File savedFile;
                try {
                    savedFile = fileRepository.save(file);
                    log.info("Saved file metadata to database: {}", savedFile.getFileId());

                    return fileMapper.toDTO(savedFile, domain);
                } catch (Exception e) {
                    log.error("Error occurred when upload multipartFile", e);
                    storageService.deleteFile(filename);
                    throw new StorageException(FILE_UPLOAD_ERROR, "Error occurred when upload the file", e);
                }
            } finally {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", tempFile, e);
                }
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Error occurred when upload multipartFile", e);
            throw new StorageException(FILE_UPLOAD_ERROR, "Error occurred when upload the file", e);
        }
    }

    @Override
    public FileDTO renameFile(UUID userId, UUID fileId, String newFilename) {
        File file = fileRepository.findByFileIdAndUserId(fileId, userId)
                .orElseThrow(() -> new NotFoundException(
                        NOT_FOUND_OR_ACCESS_DENIED_ERROR,
                        "file not found or user has no access to the file"));
        FilenameMetadata filenameMetadata = FilenameUtil.renameFile(file.getFileId(), file.getFilename(), newFilename);
        file.setFilename(filenameMetadata.getOriginalFullName());
        file.setUpdatedAt(Instant.now());
        fileRepository.save(file);

        return fileMapper.toDTO(file, domain);
    }

    @Override
    public TTFile getFile(UUID userId, UUID fileId) throws StorageException {
        File file = fileRepository.findByFileIdAndUserId(fileId, userId)
                .orElseThrow(() -> new NotFoundException(
                        NOT_FOUND_OR_ACCESS_DENIED_ERROR,
                        "file not found or user has no access to the file"));

        FilenameMetadata filenameMetadata = FilenameUtil.buildFilenameMetadata(file.getFileId(), file.getFilename());
        InputStream inputStream = storageService.getFile(filenameMetadata.getMappedFullName());

        return new TTFile(fileMapper.toDTO(file, domain), inputStream);
    }

    @Override
    public Page<FileDTO> listPublicFiles(Set<String> tags, Pageable pageable) {
        Page<File> result = (tags != null && !tags.isEmpty())
                ? fileRepository.findAllPublicFilesByTagsIn(TagsUtil.toLowercase(tags), pageable)
                : fileRepository.findAllPublicFiles(pageable);
        return new PageImpl<>(result.map(file -> fileMapper.toDTO(file, domain)).getContent(), pageable,
                result.getTotalElements());
    }

    @Override
    public Page<FileDTO> listUserFiles(UUID userId, Set<String> tags, Pageable pageable) {
        Page<File> result = (tags != null && !tags.isEmpty())
                ? fileRepository.findByUserIdAndTagsIn(userId, TagsUtil.toLowercase(tags), pageable)
                : fileRepository.findByUserId(userId, pageable);
        return new PageImpl<>(result.map(file -> fileMapper.toDTO(file, domain)).getContent(), pageable,
                result.getTotalElements());
    }

    @Override
    public void deleteFile(UUID userId, UUID fileId) {
        File file = fileRepository.findByFileIdAndUserId(fileId, userId)
                .orElseThrow(() -> new NotFoundException(
                        NOT_FOUND_OR_ACCESS_DENIED_ERROR,
                        "file not found or user has no access to the file"));
        try {
            FilenameMetadata filenameMetadata = FilenameUtil.buildFilenameMetadata(file.getFileId(),
                    file.getFilename());
            storageService.deleteFile(filenameMetadata.getMappedFullName());
        } catch (Exception e) {
            log.error("Failed to delete file '{}' from storage", file.getFilename(), e);
            throw new StorageException(DELETE_FILE_FROM_STORAGE_ERROR,
                    "error occurred when deleting file from storage", e);
        }
        try {
            fileRepository.delete(file);
        } catch (Exception e) {
            log.error("Failed to delete file information '{}' from database", file.getFilename(), e);
            throw new StorageException(DELETE_FILE_FROM_DB_ERROR,
                    "error occurred when deleting file information from database", e);
        }
    }
}
