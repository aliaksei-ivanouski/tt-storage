package com.aivanouski.ttstorage.file;

import com.aivanouski.ttstorage.TTStorageApplication;
import com.aivanouski.ttstorage.TestcontainersConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TTStorageApplication.class)
@AutoConfigureWebMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
class FileControllerIntegrationTest {

    @Container
    static GenericContainer<?> minioContainer = new GenericContainer<>("minio/minio:latest")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server", "/data");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.url", () -> minioContainer.getHost() + ":" + minioContainer.getMappedPort(9000));
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
        registry.add("minio.bucket-name", () -> "storage-files-test");
        registry.add("minio.secure", () -> false);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private UUID user1Id;
    private UUID user2Id;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
    }

    // ==================== POSITIVE TESTS ====================

    @Test
    void test1_1_ParallelUploadSameFilename() throws Exception {
        // Test 1.1: Simulate parallel UPLOAD of a file with the same FILENAME
        String filename = "test-file.txt";
        String content = "Test content for parallel upload";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        // Create two parallel upload requests
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(multipart("/api/v1/files/upload")
                                .file(file)
                                .param("userId", user1Id.toString())
                                .param("visibility", "PRIVATE")
                                .param("tags", String.join(",", Set.of("test"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);

        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(multipart("/api/v1/files/upload")
                                .file(file)
                                .param("userId", user1Id.toString())
                                .param("visibility", "PRIVATE")
                                .param("tags", String.join(",", Set.of("test"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);

        // Wait for both uploads to complete
        String result1 = future1.get(30, TimeUnit.SECONDS);
        String result2 = future2.get(30, TimeUnit.SECONDS);

        executor.shutdown();

        // Parse results
        FileDTO file1 = objectMapper.readValue(result1, FileDTO.class);
        FileDTO file2 = objectMapper.readValue(result2, FileDTO.class);

        // Verify that both files were uploaded successfully
        assertNotNull(file1.getId());
        assertNotNull(file2.getId());
        assertEquals(filename, file1.getFilename());
        assertEquals(filename, file2.getFilename());
        assertEquals(user1Id, file1.getUserId());
        assertEquals(user1Id, file2.getUserId());
    }

    @Test
    void test1_2_ParallelUploadSameContent() throws Exception {
        // Test 1.2: Simulate parallel UPLOAD of a file with the same CONTENTS
        String content = "Same content for both files";
        String filename1 = "file1.txt";
        String filename2 = "file2.txt";

        MockMultipartFile file1 = new MockMultipartFile(
                "file", filename1, "text/plain", content.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file2 = new MockMultipartFile(
                "file", filename2, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        // Create two parallel upload requests with same content but different filenames
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(multipart("/api/v1/files/upload")
                                .file(file1)
                                .param("userId", user1Id.toString())
                                .param("visibility", "PRIVATE")
                                .param("tags", String.join(",", Set.of("test"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);

        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(multipart("/api/v1/files/upload")
                                .file(file2)
                                .param("userId", user1Id.toString())
                                .param("visibility", "PRIVATE")
                                .param("tags", String.join(",", Set.of("test"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);

        // Wait for both uploads to complete
        String result1 = future1.get(30, TimeUnit.SECONDS);
        String result2 = future2.get(30, TimeUnit.SECONDS);

        executor.shutdown();

        // Parse results
        FileDTO uploadedFile1 = objectMapper.readValue(result1, FileDTO.class);
        FileDTO uploadedFile2 = objectMapper.readValue(result2, FileDTO.class);

        // Verify that both files were uploaded successfully
        assertNotNull(uploadedFile1.getId());
        assertNotNull(uploadedFile2.getId());
        assertEquals(filename1, uploadedFile1.getFilename());
        assertEquals(filename2, uploadedFile2.getFilename());
        assertEquals(user1Id, uploadedFile1.getUserId());
        assertEquals(user1Id, uploadedFile2.getUserId());
    }

    @org.junit.jupiter.api.Disabled("""
            Skipped in CI: MockMvc/MockMultipartFile cannot handle 2GB+ files in-memory. 
            Run manually with a real HTTP client/server for true large file upload validation.
            """)
    @Test
    void test1_3_UploadLargeFile() throws Exception {
        // Test 1.3: Simulate UPLOAD of a FILE that is at least 2GB size
        // Use a custom InputStream to simulate a 2GB file without allocating memory
        long fileSize = 2L * 1024 * 1024 * 1024; // 2GB
        String filename = "large-file.bin";

        class LargeFileInputStream extends java.io.InputStream {
            private long remaining = fileSize;
            private int pattern = 0;

            @Override
            public int read() {
                if (remaining-- > 0) {
                    return pattern++ % 256;
                } else {
                    return -1;
                }
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (remaining <= 0)
                    return -1;
                int toRead = (int) Math.min(len, remaining);
                for (int i = 0; i < toRead; i++) {
                    b[off + i] = (byte) (pattern++ % 256);
                }
                remaining -= toRead;
                return toRead;
            }
        }

        try (LargeFileInputStream largeStream = new LargeFileInputStream()) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "application/octet-stream", largeStream);

            String result = mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file)
                            .param("userId", user1Id.toString())
                            .param("visibility", "PRIVATE")
                            .param("tags", String.join(",", Set.of("large", "test"))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            FileDTO uploadedFile = objectMapper.readValue(result, FileDTO.class);
            assertEquals(filename, uploadedFile.getFilename());
            assertEquals(fileSize, uploadedFile.getSize());
            assertEquals(user1Id, uploadedFile.getUserId());
            assertTrue(uploadedFile.getTags().contains("large"));
            assertTrue(uploadedFile.getTags().contains("test"));
        }
    }

    @Test
    void test1_4_DeleteFileNotBelongingToUser() throws Exception {
        // Test 1.4: Try to delete file that does not belong to user

        // First, upload a file with user1
        String filename = "user1-file.txt";
        String content = "This file belongs to user1";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String uploadResult = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("test"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO uploadedFile = objectMapper.readValue(uploadResult, FileDTO.class);
        UUID fileId = uploadedFile.getId();

        // Try to delete the file with user2 (who doesn't own it)
        mockMvc.perform(delete("/api/v1/files/{fileId}/users/{userId}", fileId, user2Id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("error.file.not.found.or.access.denied"))
                .andExpect(jsonPath("$.message").value("file not found or user has no access to the file"));
    }

    @Test
    void test1_5_ListAllPublicFiles() throws Exception {
        // Upload several public files
        String[] filenames = {"public1.txt", "public2.txt", "public3.txt"};
        for (String filename : filenames) {
            String content = "Public content for " + filename;
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file)
                            .param("userId", user1Id.toString())
                            .param("visibility", "PUBLIC")
                            .param("tags", String.join(",", Set.of("public"))))
                    .andExpect(status().isOk());
        }

        // List all public files
        String result = mockMvc.perform(get("/api/v1/files/public")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse the content array from the JSON result
        List<FileDTO> files = objectMapper.readTree(result)
                .get("content")
                .traverse(objectMapper)
                .readValueAs(new TypeReference<List<FileDTO>>() {
                });

        // Verify that only public files are returned
        assertTrue(files.size() >= 3);
        files.forEach(file -> {
            assertEquals(File.Visibility.PUBLIC, file.getVisibility());
        });

        // Test filtering by tags
        String taggedResult = mockMvc.perform(get("/api/v1/files/public")
                        .param("tags", "public")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<FileDTO> taggedFiles = objectMapper.readTree(taggedResult)
                .get("content")
                .traverse(objectMapper)
                .readValueAs(new TypeReference<List<FileDTO>>() {
                });

        // Verify that only files with "public" tag are returned
        assertTrue(taggedFiles.size() >= 3);
        taggedFiles.forEach(file -> {
            assertEquals(File.Visibility.PUBLIC, file.getVisibility());
            assertTrue(file.getTags().contains("public"));
        });
    }

    // ==================== NEW POSITIVE TESTS ====================

    @Test
    void testUploadFileWithNullTags() throws Exception {
        // Test upload with null tags (should be converted to empty set)
        String filename = "no-tags.txt";
        String content = "Content without tags";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String result = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO uploadedFile = objectMapper.readValue(result, FileDTO.class);
        assertEquals(filename, uploadedFile.getFilename());
        assertEquals(user1Id, uploadedFile.getUserId());
        assertTrue(uploadedFile.getTags().isEmpty());
    }

    @Test
    void testUploadFileWithEmptyTags() throws Exception {
        // Test upload with empty tags
        String filename = "empty-tags.txt";
        String content = "Content with empty tags";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String result = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", ""))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO uploadedFile = objectMapper.readValue(result, FileDTO.class);
        assertEquals(filename, uploadedFile.getFilename());
        assertEquals(user1Id, uploadedFile.getUserId());
        assertTrue(uploadedFile.getTags().isEmpty());
    }

    @Test
    void testUploadFileWithDifferentContentTypes() throws Exception {
        // Test upload with different content types
        String[] contentTypes = {"text/plain", "application/json", "image/png", "application/pdf"};
        String[] filenames = {"text.txt", "data.json", "image.png", "document.pdf"};

        for (int i = 0; i < contentTypes.length; i++) {
            String content = "Content for " + filenames[i];
            MockMultipartFile file = new MockMultipartFile(
                    "file", filenames[i], contentTypes[i], content.getBytes(StandardCharsets.UTF_8));

            String result = mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file)
                            .param("userId", user1Id.toString())
                            .param("visibility", "PRIVATE")
                            .param("tags", "test"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            FileDTO uploadedFile = objectMapper.readValue(result, FileDTO.class);
            assertEquals(filenames[i], uploadedFile.getFilename());
            assertEquals(contentTypes[i], uploadedFile.getContentType());
            assertEquals(user1Id, uploadedFile.getUserId());
        }
    }

    @Test
    void testListUserFiles() throws Exception {
        // Upload files for user1
        String[] filenames = {"user1-file1.txt", "user1-file2.txt", "user1-file3.txt"};
        for (String filename : filenames) {
            String content = "Content for " + filename;
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file)
                            .param("userId", user1Id.toString())
                            .param("visibility", "PRIVATE")
                            .param("tags", String.join(",", Set.of("user1"))))
                    .andExpect(status().isOk());
        }

        // List user files
        String result = mockMvc.perform(get("/api/v1/files/users/{userId}", user1Id)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<FileDTO> files = objectMapper.readTree(result)
                .get("content")
                .traverse(objectMapper)
                .readValueAs(new TypeReference<List<FileDTO>>() {
                });

        // Verify that only user1's files are returned
        assertTrue(files.size() >= 3);
        files.forEach(file -> {
            assertEquals(user1Id, file.getUserId());
        });

        // Test filtering by tags
        String taggedResult = mockMvc.perform(get("/api/v1/files/users/{userId}", user1Id)
                        .param("tags", "user1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<FileDTO> taggedFiles = objectMapper.readTree(taggedResult)
                .get("content")
                .traverse(objectMapper)
                .readValueAs(new TypeReference<List<FileDTO>>() {
                });

        // Verify that only files with "user1" tag are returned
        assertTrue(taggedFiles.size() >= 3);
        taggedFiles.forEach(file -> {
            assertEquals(user1Id, file.getUserId());
            assertTrue(file.getTags().contains("user1"));
        });
    }

    @Test
    void testDownloadFile() throws Exception {
        // Upload a file first
        String filename = "download-test.txt";
        String content = "Content to download";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String uploadResult = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("test"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO uploadedFile = objectMapper.readValue(uploadResult, FileDTO.class);

        // Download the file
        mockMvc.perform(get("/api/v1/files/{fileId}/users/{userId}",
                        uploadedFile.getId(), user1Id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "form-data; name=\"attachment\"; filename=\"" + filename + "\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void testRenameFile() throws Exception {
        // Upload a file first
        String originalFilename = "original.txt";
        String content = "Content to rename";
        MockMultipartFile file = new MockMultipartFile(
                "file", originalFilename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String uploadResult = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("test"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO uploadedFile = objectMapper.readValue(uploadResult, FileDTO.class);

        // Rename the file
        String newFilename = "renamed.txt";
        FileController.RenameFilePayload renamePayload = new FileController.RenameFilePayload(
                newFilename, user1Id.toString());

        String renameResult = mockMvc.perform(put("/api/v1/files/{fileId}/rename", uploadedFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renamePayload)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO renamedFile = objectMapper.readValue(renameResult, FileDTO.class);
        // The rename operation appends the original extension to the new filename
        assertEquals(newFilename + ".txt", renamedFile.getFilename());
        assertEquals(uploadedFile.getId(), renamedFile.getId());
    }

    @Test
    void testDeleteFile() throws Exception {
        // Upload a file first
        String filename = "delete-test.txt";
        String content = "Content to delete";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String uploadResult = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("test"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO uploadedFile = objectMapper.readValue(uploadResult, FileDTO.class);

        // Delete the file
        mockMvc.perform(delete("/api/v1/files/{fileId}/users/{userId}",
                        uploadedFile.getId(), user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify the file is deleted by trying to download it
        mockMvc.perform(get("/api/v1/files/{fileId}/users/{userId}",
                        uploadedFile.getId(), user1Id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("error.file.not.found.or.access.denied"))
                .andExpect(jsonPath("$.message").value("file not found or user has no access to the file"));
    }

    @Test
    void testPaginationEdgeCases() throws Exception {
        // Test pagination with empty results
        String result = mockMvc.perform(get("/api/v1/files/public")
                        .param("page", "999")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<FileDTO> files = objectMapper.readTree(result)
                .get("content")
                .traverse(objectMapper)
                .readValueAs(new TypeReference<List<FileDTO>>() {
                });

        assertTrue(files.isEmpty());
    }

    // ==================== NEGATIVE TESTS ====================

    @Test
    void testUploadFileWithSameFilename() throws Exception {
        String filename = "test-file.txt";
        String content1 = "Test content for upload 1";
        MockMultipartFile file1 = new MockMultipartFile(
                "file", filename, "text/plain", content1.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file1)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("one"))))
                .andExpect(status().isOk());

        String content2 = "Test content for upload 2";
        MockMultipartFile file2 = new MockMultipartFile(
                "file", filename, "text/plain", content2.getBytes(StandardCharsets.UTF_8));
        // Test upload with the same filename
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file2)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("two"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.same.file"))
                .andExpect(jsonPath("$.message").value("The filename already exists"));
    }

    @Test
    void testUploadFileWithSameContent() throws Exception {
        String filename1 = "test-file_1.txt";
        String content = "Test content for upload";
        MockMultipartFile file1 = new MockMultipartFile(
                "file", filename1, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file1)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("one"))))
                .andExpect(status().isOk());

        String filename2 = "test-file_2.txt";
        MockMultipartFile file2 = new MockMultipartFile(
                "file", filename2, "text/plain", content.getBytes(StandardCharsets.UTF_8));
        // Test upload with the same filename
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file2)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("two"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.same.file"))
                .andExpect(jsonPath("$.message").value("The file content already exists"));
    }

    @Test
    void testUploadFileWithInvalidData() throws Exception {
        // Test upload with missing file
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("test"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.file.absent"))
                .andExpect(jsonPath("$.message").value("Required part 'file' is not present."));
    }

    @Test
    void testUploadFileWithInvalidUserId() throws Exception {
        String filename = "test.txt";
        String content = "Test content";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", "invalid-uuid")
                        .param("visibility", "PRIVATE")
                        .param("tags", "test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation.failed"))
                .andExpect(jsonPath("$.message").value("uploadFile.userId: must not be null or empty and must be a valid UUID"));
    }

    @Test
    void testUploadFileWithInvalidVisibility() throws Exception {
        String filename = "test.txt";
        String content = "Test content";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "INVALID")
                        .param("tags", "test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation.failed"))
                .andExpect(jsonPath("$.message").value(containsString("uploadFile.visibility: must not be null or empty and must include valid values: PUBLIC, PRIVATE")));
    }

    @Test
    void testUploadFileWithTooManyTags() throws Exception {
        String filename = "test.txt";
        String content = "Test content";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", "tag1,tag2,tag3,tag4,tag5,tag6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation.failed"))
                .andExpect(jsonPath("$.message").value("uploadFile.tags: maximum 5 tags allowed per file"));
    }

    @Test
    void testDownloadFileWithInvalidFileId() throws Exception {
        mockMvc.perform(get("/api/v1/files/{fileId}/users/{userId}",
                        "invalid-file-id", user1Id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation.failed"))
                .andExpect(jsonPath("$.message").value("getFile.fileId: must not be null or empty and must be a valid UUID"));
    }

    @Test
    void testDownloadFileWithInvalidUserId() throws Exception {
        mockMvc.perform(get("/api/v1/files/{fileId}/users/{userId}",
                        UUID.randomUUID(), "invalid-user-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation.failed"))
                .andExpect(jsonPath("$.message").value("getFile.userId: must not be null or empty and must be a valid UUID"));
    }

    @Test
    void testDownloadFileNotBelongingToUser() throws Exception {
        // Upload a file with user1
        String filename = "user1-file.txt";
        String content = "This file belongs to user1";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String uploadResult = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("test"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO uploadedFile = objectMapper.readValue(uploadResult, FileDTO.class);

        // Try to download the file with user2 (who doesn't own it)
        mockMvc.perform(get("/api/v1/files/{fileId}/users/{userId}",
                        uploadedFile.getId(), user2Id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("error.file.not.found.or.access.denied"))
                .andExpect(jsonPath("$.message").value("file not found or user has no access to the file"));
    }

    @Test
    void testDownloadNonExistentFile() throws Exception {
        mockMvc.perform(get("/api/v1/files/{fileId}/users/{userId}",
                        UUID.randomUUID(), user1Id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("error.file.not.found.or.access.denied"))
                .andExpect(jsonPath("$.message").value("file not found or user has no access to the file"));
    }

    @Test
    void testRenameFileWithInvalidFileId() throws Exception {
        FileController.RenameFilePayload renamePayload = new FileController.RenameFilePayload(
                "new-name.txt", user1Id.toString());

        mockMvc.perform(put("/api/v1/files/{fileId}/rename", "invalid-file-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renamePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation.failed"))
                .andExpect(jsonPath("$.message").value("renameFile.fileId: must not be null or empty and must be a valid UUID"));
    }

    @Test
    void testRenameFileWithInvalidPayload() throws Exception {
        // Upload a file first
        String filename = "original.txt";
        String content = "Content to rename";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String uploadResult = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("test"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO uploadedFile = objectMapper.readValue(uploadResult, FileDTO.class);

        // Test with empty filename
        FileController.RenameFilePayload emptyNamePayload = new FileController.RenameFilePayload(
                "", user1Id.toString());

        mockMvc.perform(put("/api/v1/files/{fileId}/rename", uploadedFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyNamePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.internal.server"))
                .andExpect(jsonPath("$.message").value("{\"newFilename\":\"file name must not be empty or exceed 50 symbols\"}"));

        // Test with too long filename
        String longName = "a".repeat(51);
        FileController.RenameFilePayload longNamePayload = new FileController.RenameFilePayload(
                longName, user1Id.toString());

        mockMvc.perform(put("/api/v1/files/{fileId}/rename", uploadedFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longNamePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.internal.server"))
                .andExpect(jsonPath("$.message").value("{\"newFilename\":\"file name must not be empty or exceed 50 symbols\"}"));

        // Test with invalid userId
        FileController.RenameFilePayload invalidUserPayload = new FileController.RenameFilePayload(
                "new-name.txt", "invalid-uuid");

        mockMvc.perform(put("/api/v1/files/{fileId}/rename", uploadedFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.internal.server"))
                .andExpect(jsonPath("$.message").value("{\"userId\":\"must not be null or empty and must be a valid UUID\"}"));
    }

    @Test
    void testRenameFileNotBelongingToUser() throws Exception {
        // Upload a file with user1
        String filename = "user1-file.txt";
        String content = "This file belongs to user1";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String uploadResult = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("test"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO uploadedFile = objectMapper.readValue(uploadResult, FileDTO.class);

        // Try to rename the file with user2 (who doesn't own it)
        FileController.RenameFilePayload renamePayload = new FileController.RenameFilePayload(
                "new-name.txt", user2Id.toString());

        mockMvc.perform(put("/api/v1/files/{fileId}/rename", uploadedFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renamePayload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("error.file.not.found.or.access.denied"))
                .andExpect(jsonPath("$.message").value("file not found or user has no access to the file"));
    }

    @Test
    void testDeleteFileWithInvalidFileId() throws Exception {
        mockMvc.perform(delete("/api/v1/files/{fileId}/users/{userId}",
                        "invalid-file-id", user1Id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation.failed"))
                .andExpect(jsonPath("$.message").value("deleteFile.fileId: must not be null or empty and must be a valid UUID"));
    }

    @Test
    void testDeleteFileWithInvalidUserId() throws Exception {
        mockMvc.perform(delete("/api/v1/files/{fileId}/users/{userId}",
                        UUID.randomUUID(), "invalid-user-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation.failed"))
                .andExpect(jsonPath("$.message").value("deleteFile.userId: must not be null or empty and must be a valid UUID"));
    }

    @Test
    void testDeleteNonExistentFile() throws Exception {
        mockMvc.perform(delete("/api/v1/files/{fileId}/users/{userId}",
                        UUID.randomUUID(), user1Id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("error.file.not.found.or.access.denied"))
                .andExpect(jsonPath("$.message").value("file not found or user has no access to the file"));
    }

    @Test
    void testListUserFilesWithInvalidUserId() throws Exception {
        mockMvc.perform(get("/api/v1/files/users/{userId}", "invalid-user-id")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation.failed"))
                .andExpect(jsonPath("$.message").value("listUserFiles.userId: must not be null or empty and must be a valid UUID"));
    }

    @Test
    void testListUserFilesForNonExistentUser() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        String result = mockMvc.perform(get("/api/v1/files/users/{userId}", nonExistentUserId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<FileDTO> files = objectMapper.readTree(result)
                .get("content")
                .traverse(objectMapper)
                .readValueAs(new TypeReference<List<FileDTO>>() {
                });

        assertTrue(files.isEmpty());
    }

    @Test
    void testListPublicFilesWithNonExistentTags() throws Exception {
        String result = mockMvc.perform(get("/api/v1/files/public")
                        .param("tags", "nonexistent-tag")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<FileDTO> files = objectMapper.readTree(result)
                .get("content")
                .traverse(objectMapper)
                .readValueAs(new TypeReference<List<FileDTO>>() {
                });

        assertTrue(files.isEmpty());
    }

    @Test
    void testUploadFileWithNullUserId() throws Exception {
        String filename = "test.txt";
        String content = "Test content";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("visibility", "PRIVATE")
                        .param("tags", "test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.request.param.absent"))
                .andExpect(jsonPath("$.message").value("Required request parameter 'userId' for method parameter type String is not present"));
    }

    @Test
    void testUploadFileWithNullVisibility() throws Exception {
        String filename = "test.txt";
        String content = "Test content";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("tags", "test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.request.param.absent"))
                .andExpect(jsonPath("$.message").value("Required request parameter 'visibility' for method parameter type String is not present"));
    }

    @Test
    void testRenameFileWithNullPayload() throws Exception {
        // Upload a file first
        String filename = "original.txt";
        String content = "Content to rename";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String uploadResult = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("test"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO uploadedFile = objectMapper.readValue(uploadResult, FileDTO.class);

        // Test with null payload
        mockMvc.perform(put("/api/v1/files/{fileId}/rename", uploadedFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.invalid.json"))
                .andExpect(jsonPath("$.message").value(containsString("Required request body is missing")));
    }

    @Test
    void testRenameFileWithMalformedJson() throws Exception {
        // Upload a file first
        String filename = "original.txt";
        String content = "Content to rename";
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String uploadResult = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("userId", user1Id.toString())
                        .param("visibility", "PRIVATE")
                        .param("tags", String.join(",", Set.of("test"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FileDTO uploadedFile = objectMapper.readValue(uploadResult, FileDTO.class);

        // Test with malformed JSON
        mockMvc.perform(put("/api/v1/files/{fileId}/rename", uploadedFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.invalid.json"))
                .andExpect(jsonPath("$.message").value("JSON parse error: Unexpected character ('i' (code 105)): was expecting double-quote to start field name"));
    }
}