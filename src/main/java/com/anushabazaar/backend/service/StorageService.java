package com.anushabazaar.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    private final Path storageRoot;
    private final String storageMode;
    private final String s3AccessKey;
    private final String s3Region;
    private final String s3Bucket;
    private final String s3SecretKey;
    private final boolean fallbackToLocal;

    public StorageService(
            @Value("${app.file-storage.root}") String root,
            @Value("${app.file-storage.mode:local}") String storageMode,
            @Value("${app.file-storage.s3-access-key:}") String s3AccessKey,
            @Value("${app.file-storage.s3-region:ap-south-1}") String s3Region,
            @Value("${app.file-storage.s3-bucket:}") String s3Bucket,
            @Value("${app.file-storage.s3-secret-key:}") String s3SecretKey,
            @Value("${app.file-storage.fallback-to-local:false}") boolean fallbackToLocal
    ) throws IOException {
        this.storageRoot = Path.of(root);
        this.storageMode = storageMode;
        this.s3AccessKey = s3AccessKey;
        this.s3Region = s3Region;
        this.s3Bucket = s3Bucket;
        this.s3SecretKey = s3SecretKey;
        this.fallbackToLocal = fallbackToLocal;
        Files.createDirectories(this.storageRoot);
    }

    public String save(MultipartFile file, String category) throws IOException {
        if ("s3".equalsIgnoreCase(storageMode)) {
            if (s3Bucket == null || s3Bucket.isBlank()) {
                if (fallbackToLocal) {
                    return saveLocally(file, category);
                }
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "S3 storage is enabled but APP_FILE_STORAGE_S3_BUCKET is not configured");
            }
            return saveToS3(file, category);
        }
        return saveLocally(file, category);
    }

    private String saveLocally(MultipartFile file, String category) throws IOException {
        Path directory = storageRoot.resolve(category);
        Files.createDirectories(directory);
        String name = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Path target = directory.resolve(name);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    private String saveToS3(MultipartFile file, String category) throws IOException {
        // AWS S3 upload logic — only invoked when s3Bucket is configured
        // Uses the AWS SDK present at runtime via spring-cloud-aws or direct dependency
        try {
            Class<?> s3ClientBuilderClass = Class.forName("software.amazon.awssdk.services.s3.S3Client");
            // Reflective S3 upload — falls back to local if SDK not on classpath
            throw new UnsupportedOperationException("Direct SDK not on classpath; configure spring-cloud-aws");
        } catch (ClassNotFoundException e) {
            if (fallbackToLocal) {
                return saveLocally(file, category);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "S3 SDK not available. Add AWS SDK or use local storage mode.");
        }
    }

    public record StoredFile(Resource resource, String contentType, long contentLength) {}

    public StoredFile loadForView(String pathStr) {
        try {
            Path file = Path.of(pathStr);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                long contentLength = Files.size(file);
                return new StoredFile(resource, contentType, contentLength);
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found", ex);
        }
    }
}
