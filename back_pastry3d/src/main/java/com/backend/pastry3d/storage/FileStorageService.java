package com.backend.pastry3d.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String saveModelFile(String assetKey, MultipartFile file);
    byte[] readFile(String relativePath);
    boolean exists(String relativePath);
    void deleteIfExists(String relativePath);
}
