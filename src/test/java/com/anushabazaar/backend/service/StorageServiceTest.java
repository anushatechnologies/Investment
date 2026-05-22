package com.anushabazaar.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceTest {

    @Test
    void saveReturnsClearErrorWhenS3BucketIsMissing() throws IOException {
        StorageService storageService = new StorageService(
                "target/storage-service-test",
                "s3",
                "",
                "ap-south-1",
                "anushabazaar",
                ""
        );

        MockMultipartFile file = new MockMultipartFile(
                "panCardImage",
                "pan.jpg",
                "image/jpeg",
                "test".getBytes()
        );

        assertThatThrownBy(() -> storageService.save(file, "kyc"))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(ex.getReason()).isEqualTo("S3 storage is enabled but APP_FILE_STORAGE_S3_BUCKET is not configured");
                });
    }
}
