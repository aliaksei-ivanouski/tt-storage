package com.aivanouski.ttstorage.tag;

import com.aivanouski.ttstorage.global.apidocs.SearchTagDoc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/tags")
@Tag(name = "Tags operations")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(
            summary = "List all tags",
            description = "Get a paginated list of all existent tags filtered by 'search' parameter"
    )
    @GetMapping
    public Page<String> listTags(
            @SearchTagDoc @RequestParam(required = false) String search,
            Pageable pageable) {
        log.debug("Searching for tags {}", search);
        return tagService.searchTags(search, pageable);
    }
}
