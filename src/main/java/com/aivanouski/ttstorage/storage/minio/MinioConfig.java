package com.aivanouski.ttstorage.storage.minio;

import com.aivanouski.ttstorage.error.ParseException;
import com.aivanouski.ttstorage.error.model.ErrorCodes;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.secure}")
    private boolean secure;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(getUrl())
                .credentials(accessKey, secretKey)
                .build();
    }

    private URL getUrl() {
        URL url;
        try {
            URI uri = new URI((secure ? "https://" : "http://") + minioUrl);
            url = uri.toURL();
            log.debug("Using Minio URI: {}", url);
        } catch (URISyntaxException e) {
            log.error("Failed to parse MinIO URI", e);
            throw new ParseException(ErrorCodes.PARSE_URI_ERROR, "MinIO URI is malformed.", e);
        } catch (MalformedURLException e) {
            log.error("Failed to parse MinIO URL from URI.", e);
            throw new ParseException(ErrorCodes.PARSE_URL_ERROR,
                    "Error occurred when parse MinIO URL from URI.", e);
        }
        return url;
    }
}
