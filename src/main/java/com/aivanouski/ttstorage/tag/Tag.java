package com.aivanouski.ttstorage.tag;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "tags")
@CompoundIndex(
        name = "unique_tag",
        def = "{'tag': 1}",
        unique = true
)
@Data
public class Tag {
    @Id
    private String id;

    @NotBlank(message = "Tag cannot be blank")
    @Indexed(unique = true)
    private String tagName;

    private Instant createdAt;
}
