package com.anushabazaar.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class StorageService {

    private final Path storageRoot;
    private final String storageMode;
    private final String s3AccessKey;
    private final String s3Region;
    private final String s3Bucket;
    private final String s3SecretKey;
    private final boolean fallbackToLocal;
    private final S3Client s3Client;

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
        this.s3Client = "s3".equalsIgnoreCase(storageMode) && s3Bucket != null && !s3Bucket.isBlank()
                ? createS3Client()
                : null;
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
        if (s3Client == null) {
            if (fallbackToLocal) {
                return saveLocally(file, category);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 client is not configured");
        }

        String key = category + "/" + UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(key)
                .contentType(file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType())
                .contentLength(file.getSize())
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return key;
        } catch (RuntimeException ex) {
            if (fallbackToLocal) {
                return saveLocally(file, category);
            }
            throw new IOException("S3 upload failed for " + key, ex);
        }
    }

    private S3Client createS3Client() {
        AwsCredentialsProvider credentialsProvider = s3AccessKey != null && !s3AccessKey.isBlank()
                && s3SecretKey != null && !s3SecretKey.isBlank()
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(s3AccessKey, s3SecretKey))
                : DefaultCredentialsProvider.create();
        return S3Client.builder().region(software.amazon.awssdk.regions.Region.of(s3Region)).credentialsProvider(credentialsProvider).build();
    }

    private String sanitizeFilename(String filename) {
        String safe = filename == null || filename.isBlank() ? "upload.bin" : filename;
        return safe.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record StoredFile(Resource resource, String contentType, long contentLength) {}

    public StoredFile loadForView(String pathStr) {
        try {
            if (pathStr == null || pathStr.isBlank()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File path is empty");
            }
            if (pathStr.startsWith("http://") || pathStr.startsWith("https://")) {
                Resource resource = new UrlResource(pathStr);
                if (resource.exists() || resource.isReadable()) {
                    return new StoredFile(resource, "image/jpeg", -1);
                }
            }

            if ("s3".equalsIgnoreCase(storageMode) && s3Client != null && !Path.of(pathStr).isAbsolute()) {
                ResponseInputStream<GetObjectResponse> object = s3Client.getObject(GetObjectRequest.builder().bucket(s3Bucket).key(pathStr).build());
                GetObjectResponse metadata = object.response();
                Resource resource = new InputStreamResource(object);
                String contentType = metadata.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : metadata.contentType();
                return new StoredFile(resource, contentType, metadata.contentLength() == null ? -1 : metadata.contentLength());
            }

            Path file = Path.of(pathStr);
            if (!file.isAbsolute()) {
                file = storageRoot.resolve(pathStr);
            }
            if (!Files.exists(file)) {
                // Try resolving just filename inside category subfolders
                String filename = Path.of(pathStr).getFileName().toString();
                Path kycFile = storageRoot.resolve("kyc").resolve(filename);
                if (Files.exists(kycFile)) {
                    file = kycFile;
                }
            }

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                if (contentType == null) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
                long contentLength = Files.exists(file) ? Files.size(file) : -1;
                return new StoredFile(resource, contentType, contentLength);
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + pathStr);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + pathStr, ex);
        }
    }
}
