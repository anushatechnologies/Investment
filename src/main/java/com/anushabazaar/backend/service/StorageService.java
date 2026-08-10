package com.anushabazaar.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    private final Path storageRoot;

    public StorageService(@Value("${app.file-storage.root}") String root) throws IOException {
        this.storageRoot = Path.of(root);
        Files.createDirectories(this.storageRoot);
    }

    public String save(MultipartFile file, String category) {
        try {
            Path directory = storageRoot.resolve(category);
            Files.createDirectories(directory);
            String name = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path target = directory.resolve(name);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file", ex);
        }
    }

    public record StoredFile(org.springframework.core.io.Resource resource, String contentType, long contentLength) {}

    public StoredFile loadForView(String pathStr) {
        try {
            Path file = Path.of(pathStr);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                long contentLength = Files.size(file);
                return new StoredFile(resource, contentType, contentLength);
            }
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "File not found");
        } catch (Exception ex) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "File not found", ex);
        }
    }
}
