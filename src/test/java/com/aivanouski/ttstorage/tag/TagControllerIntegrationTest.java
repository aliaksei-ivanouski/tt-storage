package com.aivanouski.ttstorage.tag;

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
import com.aivanouski.ttstorage.file.FileRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TTStorageApplication.class)
@AutoConfigureWebMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
class TagControllerIntegrationTest {

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

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private FileRepository fileRepository;

    private MockMvc mockMvc;

    private UUID user1Id;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        this.user1Id = UUID.randomUUID();
    }

    @BeforeEach
    void cleanDatabase() {
        fileRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void testListAllTags() throws Exception {
        // Upload files with different tags
        String[] filenames = {"search1.txt", "search2.txt", "other.txt"};
        String[][] tags = {{"tag1", "searchable"}, {"searchable", "tag2"}, {"other", "tag3"}};

        for (int i = 0; i < filenames.length; i++) {
            String content = "Content for " + filenames[i];
            MockMultipartFile file = new MockMultipartFile(
                    "file", filenames[i], "text/plain", content.getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file)
                            .param("filename", filenames[i])
                            .param("userId", user1Id.toString())
                            .param("visibility", "PUBLIC")
                            .param("tags", String.join(",", tags[i])))
                    .andExpect(status().isOk());
        }

        // List all tags
        String result = mockMvc.perform(get("/api/v1/tags")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> pageMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
        });
        List<String> content = objectMapper.convertValue(pageMap.get("content"), new TypeReference<List<String>>() {
        });
        Map<String, Object> pageInfo = (Map<String, Object>) pageMap.get("page");
        long totalElements = ((Number) pageInfo.get("totalElements")).longValue();

        // Verify that all tags are returned
        assertEquals(5, totalElements); // tag1, tag2, tag3, searchable, other
        assertEquals(5, content.size());
        assertTrue(content.contains("tag1"));
        assertTrue(content.contains("tag2"));
        assertTrue(content.contains("tag3"));
        assertTrue(content.contains("searchable"));
        assertTrue(content.contains("other"));
    }

    @Test
    void testSearchTags() throws Exception {
        // Upload files with searchable tags
        String[] filenames = {"search1.txt", "search2.txt", "other.txt"};
        String[][] tags = {{"tag1", "searchable"}, {"searchable", "tag2"}, {"other", "tag3"}};

        for (int i = 0; i < filenames.length; i++) {
            String content = "Content for " + filenames[i];
            MockMultipartFile file = new MockMultipartFile(
                    "file", filenames[i], "text/plain", content.getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file)
                            .param("filename", filenames[i])
                            .param("userId", user1Id.toString())
                            .param("visibility", "PUBLIC")
                            .param("tags", String.join(",", tags[i])))
                    .andExpect(status().isOk());
        }

        // Search for tags containing "search"
        String result = mockMvc.perform(get("/api/v1/tags")
                        .param("search", "search")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> pageMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
        });
        List<String> content = objectMapper.convertValue(pageMap.get("content"), new TypeReference<List<String>>() {
        });
        Map<String, Object> pageInfo = (Map<String, Object>) pageMap.get("page");
        long totalElements = ((Number) pageInfo.get("totalElements")).longValue();

        // Verify that only searchable tags are returned
        assertTrue(totalElements >= 1);
        assertTrue(content.contains("searchable"));
        content.forEach(tag -> assertTrue(tag.contains("search")));
    }

    @Test
    void testListTagsWithPagination() throws Exception {
        // Upload many files to test pagination
        for (int i = 1; i <= 5; i++) {
            String filename = "page" + i + ".txt";
            String content = "Content for " + filename;
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file)
                            .param("filename", filename)
                            .param("userId", user1Id.toString())
                            .param("visibility", "PUBLIC")
                            .param("tags", "page" + i))
                    .andExpect(status().isOk());
        }

        // Test pagination with page size 2
        String result = mockMvc.perform(get("/api/v1/tags")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> pageMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
        });
        List<String> content = objectMapper.convertValue(pageMap.get("content"), new TypeReference<List<String>>() {
        });
        Map<String, Object> pageInfo = (Map<String, Object>) pageMap.get("page");
        long totalElements = ((Number) pageInfo.get("totalElements")).longValue();
        boolean hasNext = pageInfo.get("number") != null
                && ((Number) pageInfo.get("number")).intValue() < ((Number) pageInfo.get("totalPages")).intValue() - 1;
        boolean hasPrevious = pageInfo.get("number") != null && ((Number) pageInfo.get("number")).intValue() > 0;

        // Verify pagination
        assertEquals(2, content.size());
        assertTrue(totalElements >= 5);
        assertTrue(hasNext);
        assertFalse(hasPrevious);
    }

    @Test
    void testListTagsWithSorting() throws Exception {
        // Upload files with different tag names for sorting
        String[] filenames = {"sort1.txt", "sort2.txt", "sort3.txt"};
        String[] tags = {"zebra", "apple", "banana"};

        for (int i = 0; i < filenames.length; i++) {
            String content = "Content for " + filenames[i];
            MockMultipartFile file = new MockMultipartFile(
                    "file", filenames[i], "text/plain", content.getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file)
                            .param("filename", filenames[i])
                            .param("userId", user1Id.toString())
                            .param("visibility", "PUBLIC")
                            .param("tags", tags[i]))
                    .andExpect(status().isOk());
        }

        // Test sorting by name ascending
        String result = mockMvc.perform(get("/api/v1/tags")
                        .param("sort", "tagName,asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> pageMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
        });
        List<String> content = objectMapper.convertValue(pageMap.get("content"), new TypeReference<List<String>>() {
        });

        // Verify sorting (should be alphabetical: apple, banana, zebra)
        assertTrue(content.size() >= 3);
        // Check that apple comes before banana and banana comes before zebra
        int appleIndex = content.indexOf("apple");
        int bananaIndex = content.indexOf("banana");
        int zebraIndex = content.indexOf("zebra");

        assertTrue(appleIndex >= 0);
        assertTrue(bananaIndex >= 0);
        assertTrue(zebraIndex >= 0);
        assertTrue(appleIndex < bananaIndex);
        assertTrue(bananaIndex < zebraIndex);
    }

    @Test
    void testListTagsWithEmptyResult() throws Exception {
        // Search for a tag that doesn't exist
        String result = mockMvc.perform(get("/api/v1/tags")
                        .param("search", "nonexistenttag")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> pageMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
        });
        List<String> content = objectMapper.convertValue(pageMap.get("content"), new TypeReference<List<String>>() {
        });
        Map<String, Object> pageInfo = (Map<String, Object>) pageMap.get("page");
        long totalElements = ((Number) pageInfo.get("totalElements")).longValue();

        // Verify empty result
        assertEquals(0, content.size());
        assertEquals(0, totalElements);
    }
}