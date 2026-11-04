package subastix.web;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class ImageStorage {
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private ImageStorage() {
    }

    public static StoredImage store(Part part) throws IOException, InvalidImageException {
        if (part == null || part.getSize() == 0) return null;
        if (part.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidImageException("La imagen debe pesar 10 MB como máximo.");
        }

        String contentType = part.getContentType();
        byte[] signature;
        try (InputStream input = part.getInputStream()) {
            signature = input.readNBytes(8);
        }
        String extension;
        if ("image/jpeg".equalsIgnoreCase(contentType)
                && signature.length >= 3 && (signature[0] & 0xff) == 0xff
                && (signature[1] & 0xff) == 0xd8 && (signature[2] & 0xff) == 0xff) {
            extension = ".jpg";
        } else if ("image/png".equalsIgnoreCase(contentType) && isPng(signature)) {
            extension = ".png";
        } else {
            throw new InvalidImageException("El archivo debe ser una imagen JPG o PNG válida.");
        }

        String filename = UUID.randomUUID() + extension;
        Path directory = uploadDirectory();
        Files.createDirectories(directory);
        Path target = directory.resolve(filename);
        try (InputStream input = part.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return new StoredImage(filename, "/images/" + filename, target);
    }

    public static Path uploadDirectory() {
        String configured = System.getenv("UPLOAD_DIR");
        if (configured != null && !configured.isBlank()) return Path.of(configured).toAbsolutePath().normalize();
        String catalinaBase = System.getProperty("catalina.base", System.getProperty("java.io.tmpdir"));
        return Path.of(catalinaBase, "subastix-uploads").toAbsolutePath().normalize();
    }

    private static boolean isPng(byte[] signature) {
        if (signature.length < 8) return false;
        byte[] expected = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        return java.util.Arrays.equals(expected, signature);
    }

    public record StoredImage(String filename, String url, Path path) {
    }

    public static class InvalidImageException extends Exception {
        public InvalidImageException(String message) {
            super(message);
        }
    }
}
