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
}
