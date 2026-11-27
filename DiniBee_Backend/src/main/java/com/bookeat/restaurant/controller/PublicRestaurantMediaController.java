package com.bookeat.restaurant.controller;

import com.bookeat.restaurant.enums.DocumentType;
import com.bookeat.restaurant.repository.RestaurantDocumentRepository;
import com.bookeat.restaurant.storage.DocumentStorageRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/restaurants")
public class PublicRestaurantMediaController {
    private final RestaurantDocumentRepository documents;
    private final DocumentStorageRouter storage;
    private final com.bookeat.restaurant.repository.RestaurantRepository restaurants;

    @GetMapping("/{restaurantId}/media/{documentId}")
    public ResponseEntity<byte[]> media(@PathVariable UUID restaurantId, @PathVariable UUID documentId) {
        var requested = documents.findByIdAndRestaurantIdAndDeletedAtIsNull(documentId, restaurantId).orElse(null);
        DocumentType wantedType = requested == null ? null : requested.getDocumentType();
        if (wantedType == null) {
            var restaurant = restaurants.findById(restaurantId).orElse(null);
            if (restaurant == null) return ResponseEntity.notFound().build();
            String suffix = "/media/" + documentId;
            if (restaurant.getLogoUrl() != null && restaurant.getLogoUrl().endsWith(suffix)) wantedType = DocumentType.LOGO;
            else if (restaurant.getCoverImageUrl() != null && restaurant.getCoverImageUrl().endsWith(suffix)) wantedType = DocumentType.COVER_IMAGE;
        }
        if (wantedType == null || !publicImage(wantedType)) return ResponseEntity.notFound().build();

        // A restaurant can have an old image row whose local/object-storage file was
        // removed. Try the requested image first, then another live image belonging
        // to the SAME restaurant. Never leak media across restaurants and never turn
        // a storage exception into a 503 on the customer home/detail screen.
        var candidates = new java.util.ArrayList<com.bookeat.restaurant.entity.RestaurantDocument>();
        if (requested != null) candidates.add(requested);
        final DocumentType requiredType = wantedType;
        documents.findByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(restaurantId).stream()
                .filter(d -> d.getDocumentType() == requiredType)
                .filter(d -> requested == null || !d.getId().equals(requested.getId()))
                .sorted(java.util.Comparator.comparing(com.bookeat.restaurant.entity.RestaurantDocument::getCreatedAt).reversed())
                .forEach(candidates::add);
        for (var d : candidates) {
            try {
                byte[] bytes = storage.forProvider(d.getStorageProvider()).read(d.getFileKey());
                if (bytes == null || bytes.length == 0) continue;
                MediaType type;
                try { type = MediaType.parseMediaType(d.getContentType()); }
                catch (Exception ignored) { type = MediaType.APPLICATION_OCTET_STREAM; }
                return ResponseEntity.ok().contentType(type)
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic()).body(bytes);
            } catch (RuntimeException ignored) { }
        }
        return ResponseEntity.notFound().build();
    }
    private boolean publicImage(DocumentType type){return type==DocumentType.LOGO || type==DocumentType.COVER_IMAGE;}
}
