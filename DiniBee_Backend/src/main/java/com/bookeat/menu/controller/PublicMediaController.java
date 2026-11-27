package com.bookeat.menu.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/public/media")
public class PublicMediaController {

    private final Path mediaRoot = Path.of(System.getProperty("dinibee.menu.media.root", System.getProperty("user.home") + "/.dinibee/menu-images")).toAbsolutePath().normalize();

    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> getMedia(@PathVariable String filename) {
        // Prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        Path file = mediaRoot.resolve(filename).normalize();
        if (!file.startsWith(mediaRoot) || !Files.exists(file) || !Files.isRegularFile(file)) {
            // v24 compatibility: locate files uploaded by older builds before the persistent
            // media root was introduced. New uploads always use mediaRoot.
            Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
            Path[] legacyRoots = new Path[]{
                    userDir.resolve("menu-images"),
                    userDir.resolve("uploads"),
                    userDir.resolve("private-documents"),
                    userDir.resolve("storage/menu-images")
            };
            file = null;
            for (Path legacy : legacyRoots) {
                Path candidate = legacy.resolve(filename).normalize();
                if (candidate.startsWith(legacy.normalize()) && Files.exists(candidate) && Files.isRegularFile(candidate)) {
                    file = candidate;
                    break;
                }
            }
            if (file == null) return ResponseEntity.notFound().build();
        }

        try {
            byte[] bytes = Files.readAllBytes(file);
            String lower = filename.toLowerCase();
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (lower.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            } else if (lower.endsWith(".webp")) {
                mediaType = MediaType.parseMediaType("image/webp");
            } else if (lower.endsWith(".gif")) {
                mediaType = MediaType.IMAGE_GIF;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
