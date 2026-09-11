package com.veterinaria.backend.peluqueria.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    StoredImage upload(MultipartFile file, String folder, String generatedName);

    void delete(String storageKey);
}
