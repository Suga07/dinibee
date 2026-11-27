package com.bookeat.restaurant.controller;
import com.bookeat.restaurant.service.RestaurantDocumentService.Download;
import org.springframework.http.*;
final class DocumentDownloadResponse {
    private DocumentDownloadResponse() {}
    static ResponseEntity<byte[]> of(Download file) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.filename()).build().toString())
            .header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(file.bytes());
    }
}
