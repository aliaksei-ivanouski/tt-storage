package com.aivanouski.ttstorage.tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface TagService {
    Page<String> searchTags(String search, Pageable pageable);

    void register(Set<String> tags);
}
