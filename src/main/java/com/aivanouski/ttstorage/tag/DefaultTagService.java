package com.aivanouski.ttstorage.tag;

import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class DefaultTagService implements TagService {

    private static final String TAGS_COLLECTION = "tags";

    private final TagRepository tagRepository;
    private final MongoTemplate mongoTemplate;

    public DefaultTagService(TagRepository tagRepository, MongoTemplate mongoTemplate) {
        this.tagRepository = tagRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<String> searchTags(String search, Pageable pageable) {
        return (search != null && !search.isEmpty()
                ? new PageImpl<>(tagRepository.findAllBySearch(search, pageable).getContent().stream()
                .map(Tag::getTagName).toList(), pageable,
                tagRepository.findAllBySearch(search, pageable).getTotalElements())
                : new PageImpl<>(tagRepository.findAll(pageable).getContent().stream().map(Tag::getTagName).toList(),
                pageable, tagRepository.findAll(pageable).getTotalElements()));
    }

    @Override
    public void register(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            log.info("No tags provided");
            return;
        }

        List<WriteModel<Document>> models = tags.stream()
                .map(tag -> (WriteModel<Document>) new UpdateOneModel<Document>(
                        Filters.eq("tagName", tag),
                        Updates.combine(
                                Updates.setOnInsert("tagName", tag),
                                Updates.setOnInsert("createdAt", Instant.now())),
                        new UpdateOptions().upsert(true)))
                .toList();
        BulkWriteOptions options = new BulkWriteOptions().ordered(false);
        mongoTemplate.getCollection(TAGS_COLLECTION).bulkWrite(models, options);
    }
}
