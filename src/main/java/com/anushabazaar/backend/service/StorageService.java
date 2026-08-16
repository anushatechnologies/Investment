package com.anushabazaar.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path storageRoot;
    private final String storageMode;
    private final String s3AccessKey;
    private final String s3Region;
    private final String s3Bucket;
    private final String s3SecretKey;
    private final boolean fallbackToLocal;
    private final S3Client s3Client;

    public StorageService(
            @Value("${app.file-storage.root:uploads}") String root,
            @Value("${app.file-storage.mode:local}") String storageMode,
            @Value("${app.file-storage.s3-access-key:}") String s3AccessKey,
            @Value("${app.file-storage.s3-region:ap-south-2}") String s3Region,
            @Value("${app.file-storage.s3-bucket:}") String s3Bucket,
            @Value("${app.file-storage.s3-secret-key:}") String s3SecretKey,
            @Value("${app.file-storage.fallback-to-local:true}") boolean fallbackToLocal
    ) throws IOException {
        this.storageRoot = Path.of(root);
        this.storageMode = storageMode;
        this.s3AccessKey = s3AccessKey;
        this.s3Region = (s3Region == null || s3Region.isBlank()) ? "ap-south-2" : s3Region;
        this.s3Bucket = s3Bucket;
        this.s3SecretKey = s3SecretKey;
        this.fallbackToLocal = fallbackToLocal;

        try {
            Files.createDirectories(this.storageRoot);
        } catch (Exception e) {
            log.warn("Could not create local storage directory {}: {}", this.storageRoot, e.getMessage());
        }

        boolean isS3Enabled = isS3Configured(storageMode, s3Bucket);
        if (isS3Enabled) {
            log.info("Initializing AWS S3 Client for bucket '{}' in region '{}'...", s3Bucket, this.s3Region);
            this.s3Client = createS3Client();
            log.info("AWS S3 Client successfully initialized for bucket '{}'", s3Bucket);
        } else {
            log.info("StorageService running in LOCAL storage mode. Root: {}", this.storageRoot.toAbsolutePath());
            this.s3Client = null;
        }
    }

    private boolean isS3Configured(String mode, String bucket) {
        if (bucket != null && !bucket.isBlank()) {
            return true;
        }
        return "s3".equalsIgnoreCase(mode) || "aws".equalsIgnoreCase(mode);
    }

    public String save(MultipartFile file, String category) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot upload empty file");
        }

        if (s3Client != null && s3Bucket != null && !s3Bucket.isBlank()) {
            return saveToS3(file, category);
        }

        return saveLocally(file, category);
    }

    private String saveLocally(MultipartFile file, String category) throws IOException {
        Path directory = storageRoot.resolve(category);
        Files.createDirectories(directory);
        String name = UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        Path target = directory.resolve(name);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Saved file locally at: {}", target);
        return category + "/" + name;
    }

    private String saveToS3(MultipartFile file, String category) throws IOException {
        String key = category + "/" + UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        String contentType = file.getContentType() == null || file.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : file.getContentType();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();

        try {
            byte[] fileBytes = file.getBytes();
            s3Client.putObject(request, RequestBody.fromBytes(fileBytes));
            log.info("Successfully uploaded file to S3: s3://{}/{}", s3Bucket, key);
            return key;
        } catch (Exception ex) {
            log.error("S3 upload failed for key '{}' in bucket '{}': {}", key, s3Bucket, ex.getMessage(), ex);
            if (fallbackToLocal) {
                log.warn("Falling back to local storage for category '{}'...", category);
                return saveLocally(file, category);
            }
            throw new IOException("S3 upload failed for " + key + ": " + ex.getMessage(), ex);
        }
    }

    private S3Client createS3Client() {
        AwsCredentialsProvider credentialsProvider;
        if (s3AccessKey != null && !s3AccessKey.isBlank() && s3SecretKey != null && !s3SecretKey.isBlank()) {
            log.info("Using static AWS credentials (access key provided)");
            credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(s3AccessKey.trim(), s3SecretKey.trim()));
        } else {
            log.info("Using Default AWS Credentials Provider chain (IAM Role / Environment)");
            credentialsProvider = DefaultCredentialsProvider.create();
        }

        return S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(s3Region.trim()))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    private String sanitizeFilename(String filename) {
        String safe = filename == null || filename.isBlank() ? "upload.jpg" : filename;
        return safe.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record StoredFile(Resource resource, String contentType, long contentLength) {}

    public StoredFile loadForView(String pathStr) {
        if (pathStr == null || pathStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File path is empty");
        }

        // 1. Direct HTTP/HTTPS URLs
        if (pathStr.startsWith("http://") || pathStr.startsWith("https://")) {
            try {
                Resource resource = new UrlResource(pathStr);
                if (resource.exists() || resource.isReadable()) {
                    return new StoredFile(resource, "image/jpeg", -1);
                }
            } catch (Exception ignored) {}
        }

        // Normalize path (strip leading slashes, backslashes, uploads prefix)
        String normalizedKey = pathStr.replace("\\", "/")
                .replaceFirst("^[/]+", "")
                .replaceFirst("^uploads/", "")
                .replaceFirst("^app/uploads/", "");

        // 2. Try loading from S3 if configured
        if (s3Client != null && s3Bucket != null && !s3Bucket.isBlank()) {
            try {
                ResponseInputStream<GetObjectResponse> object = s3Client.getObject(
                        GetObjectRequest.builder().bucket(s3Bucket).key(normalizedKey).build()
                );
                GetObjectResponse metadata = object.response();
                Resource resource = new InputStreamResource(object);
                String contentType = metadata.contentType() == null ? MediaType.IMAGE_JPEG_VALUE : metadata.contentType();
                long contentLength = metadata.contentLength() == null ? -1 : metadata.contentLength();
                return new StoredFile(resource, contentType, contentLength);
            } catch (Exception s3Ex) {
                log.debug("Could not fetch key '{}' from S3: {}. Trying local disk fallback...", normalizedKey, s3Ex.getMessage());
            }
        }

        // 3. Try loading from Local Storage
        try {
            Path file = storageRoot.resolve(normalizedKey);
            if (!Files.exists(file)) {
                // Try resolving just filename inside category subfolders
                String filename = Path.of(normalizedKey).getFileName().toString();
                Path kycFile = storageRoot.resolve("kyc").resolve(filename);
                if (Files.exists(kycFile)) {
                    file = kycFile;
                } else {
                    Path receiptFile = storageRoot.resolve("receipts").resolve(filename);
                    if (Files.exists(receiptFile)) {
                        file = receiptFile;
                    }
                }
            }

            if (Files.exists(file) && Files.isReadable(file)) {
                String contentType = Files.probeContentType(file);
                if (contentType == null) {
                    contentType = MediaType.IMAGE_JPEG_VALUE;
                }
                long contentLength = Files.size(file);
                Resource resource = new UrlResource(file.toUri());
                return new StoredFile(resource, contentType, contentLength);
            }
        } catch (Exception localEx) {
            log.debug("Could not fetch file locally: {}", localEx.getMessage());
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found: " + pathStr);
    }
}
