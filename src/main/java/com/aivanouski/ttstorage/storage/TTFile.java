package com.aivanouski.ttstorage.storage;

import com.aivanouski.ttstorage.file.FileDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.InputStream;

@AllArgsConstructor
@Getter
public class TTFile {
    FileDTO metadata;
    InputStream inputStream;
}