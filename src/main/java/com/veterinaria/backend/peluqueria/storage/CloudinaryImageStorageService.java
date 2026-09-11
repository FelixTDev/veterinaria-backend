package com.veterinaria.backend.peluqueria.storage;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryImageStorageService implements ImageStorageService {

    private final CloudinaryProperties properties;
    private final Cloudinary cloudinary;

    public CloudinaryImageStorageService(CloudinaryProperties properties) {
        this.properties = properties;
        this.cloudinary = new Cloudinary(Map.of(
                "cloud_name", valueOrEmpty(properties.cloudName()),
                "api_key", valueOrEmpty(properties.apiKey()),
                "api_secret", valueOrEmpty(properties.apiSecret())));
    }

    @Override
    public StoredImage upload(MultipartFile file, String folder, String generatedName) {
        requireConfigured();
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "folder", folder,
                            "public_id", generatedName,
                            "overwrite", false,
                            "use_filename", false,
                            "unique_filename", false));
            Object secureUrl = result.get("secure_url");
            Object publicId = result.get("public_id");
            if (secureUrl == null || publicId == null) {
                throw new IllegalStateException("Cloudinary no devolvio metadata completa del asset.");
            }
            return new StoredImage(secureUrl.toString(), publicId.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo almacenar la imagen.", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        requireConfigured();
        try {
            cloudinary.uploader().destroy(storageKey, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo eliminar el asset compensatorio.", exception);
        }
    }

    private void requireConfigured() {
        if (isBlank(properties.cloudName()) || isBlank(properties.apiKey()) || isBlank(properties.apiSecret())) {
            throw new IllegalStateException("Cloudinary no esta configurado.");
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
