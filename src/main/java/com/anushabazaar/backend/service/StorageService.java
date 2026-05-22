package com.anushabazaar.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class StorageService {

    private final Path storageRoot;
    private final String provider;
    private final String bucket;
    private final String prefix;
    private final String publicBaseUrl;
    private final S3Client s3Client;

    public StorageService(@Value("${app.file-storage.root}") String root,
                          @Value("${app.file-storage.provider:local}") String provider,
                          @Value("${app.file-storage.s3.bucket:}") String bucket,
                          @Value("${app.file-storage.s3.region:ap-south-1}") String region,
                          @Value("${app.file-storage.s3.prefix:anushabazaar}") String prefix,
                          @Value("${app.file-storage.s3.public-base-url:}") String publicBaseUrl) throws IOException {
        this.storageRoot = Path.of(root);
        this.provider = provider == null ? "local" : provider.toLowerCase(Locale.ROOT);
        this.bucket = bucket;
        this.prefix = normalizePrefix(prefix);
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.strip();
        this.s3Client = "s3".equals(this.provider)
                ? S3Client.builder().region(Region.of(region)).build()
                : null;
        if (!"s3".equals(this.provider)) {
            Files.createDirectories(this.storageRoot);
        }
    }

    public String save(MultipartFile file, String category) {
        if ("s3".equals(provider)) {
            return saveToS3(file, category);
        }
        return saveToLocal(file, category);
    }

    private String saveToLocal(MultipartFile file, String category) {
        try {
            Path directory = storageRoot.resolve(category);
            Files.createDirectories(directory);
            String name = safeName(file.getOriginalFilename());
            Path target = directory.resolve(name);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file: " + ex.getMessage(), ex);
        }
    }

    private String saveToS3(MultipartFile file, String category) {
        if (bucket == null || bucket.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "S3 storage is enabled but APP_FILE_STORAGE_S3_BUCKET is not configured"
            );
        }
        String key = prefix + "/" + category + "/" + safeName(file.getOriginalFilename());
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            if (!publicBaseUrl.isBlank()) {
                return publicBaseUrl.replaceAll("/+$", "") + "/" + key;
            }
            return "s3://" + bucket + "/" + key;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file for S3 upload: " + ex.getMessage(), ex);
        } catch (SdkException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to upload file to S3: " + ex.getMessage(), ex);
        }
    }

    private String normalizePrefix(String value) {
        if (value == null || value.isBlank()) {
            return "uploads";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String safeName(String originalFilename) {
        String original = originalFilename == null || originalFilename.isBlank() ? "upload" : originalFilename;
        String sanitized = Path.of(original).getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        return UUID.randomUUID() + "-" + sanitized;
    }
}
