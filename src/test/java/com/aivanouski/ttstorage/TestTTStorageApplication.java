package com.aivanouski.ttstorage;

import org.springframework.boot.SpringApplication;

public class TestTTStorageApplication {

    public static void main(String[] args) {
        SpringApplication.from(TTStorageApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
