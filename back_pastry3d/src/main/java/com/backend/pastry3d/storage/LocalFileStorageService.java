package com.backend.pastry3d.storage;

import com.backend.pastry3d.shared.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class LocalFileStorageService implements FileStorageService {
    private final String basePath;

    public LocalFileStorageService(@Value("${storage.uploads:uploads/models}") String basePath) {
        this.basePath = basePath;
    }

    @Override
    public String saveModelFile(String assetKey, MultipartFile file) {
        validateGlb(file);
        String safeKey = sanitize(assetKey);
        Path directory = Path.of(basePath, "imported");
        Path filePath = directory.resolve(safeKey + ".glb");
        try {
            Files.createDirectories(directory);
            Files.write(filePath, file.getBytes());
            return "/uploads/models/imported/" + safeKey + ".glb";
        } catch (IOException exception) {
            throw new BadRequestException("No se pudo guardar el modelo GLB");
        }
    }

    @Override
    public byte[] readFile(String relativePath) {
        try {
            return Files.readAllBytes(resolve(relativePath));
        } catch (IOException exception) {
            throw new BadRequestException("No se pudo leer el archivo");
        }
    }

    @Override
    public boolean exists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return false;
        return Files.exists(resolve(relativePath));
    }

    @Override
    public void deleteIfExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException exception) {
            throw new BadRequestException("No se pudo eliminar el archivo");
        }
    }

    private Path resolve(String relativePath) {
        String cleaned = relativePath.replace("/uploads/models/", "");
        return Path.of(basePath).resolve(cleaned).normalize();
    }

    private void validateGlb(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("El archivo GLB es obligatorio");
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".glb")) {
            throw new BadRequestException("Solo se aceptan archivos .glb");
        }
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) return "model";
        return value.trim().toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }
}
