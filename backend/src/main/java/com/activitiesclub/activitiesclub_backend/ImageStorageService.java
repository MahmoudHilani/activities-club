package com.activitiesclub.activitiesclub_backend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;

@Service
public class ImageStorageService {
    public static final String PLACEHOLDER_FILENAME = "placeholder-activity.svg";
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Map<String, String> EXTENSIONS = Map.of(
        "image/jpeg", ".jpg",
        "image/png", ".png",
        "image/webp", ".webp"
    );

    private final Path uploadsDir;

    public ImageStorageService(@Value("${app.uploads.dir}") String uploadsDir) {
        this.uploadsDir = Path.of(uploadsDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(uploadsDir);
            Path placeholder = uploadsDir.resolve(PLACEHOLDER_FILENAME);
            if (Files.notExists(placeholder)) {
                Files.writeString(placeholder, placeholderSvg(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize uploads directory", exception);
        }
    }

    public String storeRequiredImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activity image is required");
        }

        return storeImage(image);
    }

    public String replaceImage(String currentImagePath, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return currentImagePath;
        }

        String nextImagePath = storeImage(image);
        deleteManagedImage(currentImagePath);
        return nextImagePath;
    }

    public void deleteManagedImage(String imagePath) {
        if (!StringUtils.hasText(imagePath) || PLACEHOLDER_FILENAME.equals(imagePath)) {
            return;
        }

        try {
            Files.deleteIfExists(uploadsDir.resolve(imagePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete stored image", exception);
        }
    }

    private String storeImage(MultipartFile image) {
        String contentType = image.getContentType();
        String extension = EXTENSIONS.get(contentType);

        if (extension == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Image must be a JPEG, PNG, or WebP file"
            );
        }
        if (image.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must be 5 MB or smaller");
        }

        String filename = UUID.randomUUID() + extension;
        Path target = uploadsDir.resolve(filename);

        try {
            Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store activity image", exception);
        }

        return filename;
    }

    private String placeholderSvg() {
        return """
            <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="800" viewBox="0 0 1200 800">
              <defs>
                <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
                  <stop offset="0%" stop-color="#f3efe4" />
                  <stop offset="100%" stop-color="#dbe7ff" />
                </linearGradient>
              </defs>
              <rect width="1200" height="800" fill="url(#bg)" rx="48" />
              <circle cx="210" cy="190" r="110" fill="#f4b35d" opacity="0.55" />
              <circle cx="960" cy="620" r="140" fill="#5d7df4" opacity="0.18" />
              <text x="96" y="372" fill="#243245" font-family="Arial, sans-serif" font-size="72" font-weight="700">
                Activities Club
              </text>
              <text x="96" y="446" fill="#52606f" font-family="Arial, sans-serif" font-size="34">
                Activity image pending upload
              </text>
            </svg>
            """;
    }
}
