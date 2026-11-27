package com.bookeat.restaurant.service;
import com.bookeat.restaurant.dto.RestaurantRequests.*;
import com.bookeat.restaurant.dto.RestaurantResponses.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
public interface RestaurantDocumentService {
    Detail upload(UUID actor,UUID id,DocumentUpload metadata,MultipartFile file);
    Detail delete(UUID actor,UUID id,UUID documentId,long version);
    List<DocumentView> listOwner(UUID actor,UUID id);
    List<DocumentView> listAdmin(UUID id);
    Download downloadOwner(UUID actor,UUID id,UUID documentId);
    Download downloadAdmin(UUID id,UUID documentId);
    Detail verify(UUID actor,UUID id,UUID documentId,Verification request);
    record Download(String filename,byte[] bytes) {}
}
