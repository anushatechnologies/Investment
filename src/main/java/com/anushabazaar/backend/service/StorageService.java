package com.anushabazaar.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path storageRoot;
    private final String provider;
    private final String bucket;
    private final String prefix;
    private final String publicBaseUrl;
    private final boolean fallbackToLocal;
    private final S3Client s3Client;

    public StorageService(@Value("${app.file-storage.root}") String root,
                          @Value("${app.file-storage.provider:local}") String provider,
                          @Value("${app.file-storage.s3.bucket:}") String bucket,
                          @Value("${app.file-storage.s3.region:ap-south-1}") String region,
                          @Value("${app.file-storage.s3.prefix:anushabazaar}") String prefix,
                          @Value("${app.file-storage.s3.public-base-url:}") String publicBaseUrl,
                          @Value("${app.file-storage.s3.fallback-to-local:true}") boolean fallbackToLocal) throws IOException {
        this.storageRoot = Path.of(root);
        this.provider = provider == null ? "local" : provider.toLowerCase(Locale.ROOT);
        this.bucket = bucket;
        this.prefix = normalizePrefix(prefix);
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.strip();
        this.fallbackToLocal = fallbackToLocal;
        this.s3Client = "s3".equals(this.provider)
                ? S3Client.builder().region(Region.of(region)).build()
                : null;
        if (!"s3".equals(this.provider) || fallbackToLocal) {
            Files.createDirectories(this.storageRoot);
        }
    }

    public String save(MultipartFile file, String category) {
        if ("s3".equals(provider)) {
            return saveToS3(file, category);
        }
        return saveToLocal(file, category);
    }

    public Resource loadAsResource(String path) {
        return loadForView(path).resource();
    }

    public StoredFile loadForView(String path) {
        if ("s3".equals(provider)) {
            return loadFromS3(path);
        }
        return loadFromLocal(path);
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
            if (fallbackToLocal) {
                log.warn("S3 storage is enabled but bucket is not configured. Falling back to local storage.");
                return saveToLocal(file, category);
            }
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
            if (fallbackToLocal) {
                log.warn("S3 upload failed. Falling back to local storage. Reason: {}", ex.getMessage());
                return saveToLocal(file, category);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to upload file to S3: " + ex.getMessage(), ex);
        }
    }

    private StoredFile loadFromLocal(String path) {
        try {
            Path root = storageRoot.toAbsolutePath().normalize();
            Path requested = Path.of(path);
            Path file = requested.isAbsolute()
                    ? requested.toAbsolutePath().normalize()
                    : root.resolve(stripStorageRoot(path)).normalize();
            if (!file.startsWith(root)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
            }
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            String contentType = Files.probeContentType(file);
            return new StoredFile(new UrlResource(file.toUri()), contentType, Files.size(file));
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path", ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load file: " + ex.getMessage(), ex);
        }
    }

    private StoredFile loadFromS3(String path) {
        if (bucket == null || bucket.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "S3 storage is enabled but APP_FILE_STORAGE_S3_BUCKET is not configured"
            );
        }
        String key = normalizeS3Key(path);
        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            Resource resource = new InputStreamResource(s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()));
            return new StoredFile(resource, head.contentType(), head.contentLength());
        } catch (NoSuchKeyException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found", ex);
        } catch (SdkException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to load file from S3: " + ex.getMessage(), ex);
        }
    }

    private String normalizePrefix(String value) {
        if (value == null || value.isBlank()) {
            return "uploads";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String normalizeS3Key(String path) {
        String value = path == null ? "" : path.strip().replace('\\', '/');
        if (value.isBlank() || value.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
        if (!publicBaseUrl.isBlank() && value.startsWith(publicBaseUrl)) {
            value = value.substring(publicBaseUrl.length());
        }
        if (value.startsWith("s3://")) {
            value = value.substring("s3://".length());
        }
        value = value.replaceAll("^/+", "");
        if (value.startsWith(bucket + "/")) {
            value = value.substring(bucket.length() + 1);
        }
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
        return value;
    }

    private String stripStorageRoot(String path) {
        String normalizedPath = path.replace('\\', '/').replaceAll("^/+", "");
        String normalizedRoot = storageRoot.toString().replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
        if (normalizedPath.equals(normalizedRoot)) {
            return "";
        }
        if (normalizedPath.startsWith(normalizedRoot + "/")) {
            return normalizedPath.substring(normalizedRoot.length() + 1);
        }
        return normalizedPath;
    }

    private String safeName(String originalFilename) {
        String original = originalFilename == null || originalFilename.isBlank() ? "upload" : originalFilename;
        String sanitized = Path.of(original).getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        return UUID.randomUUID() + "-" + sanitized;
    }

    public record StoredFile(Resource resource, String contentType, long contentLength) {
    }
}
