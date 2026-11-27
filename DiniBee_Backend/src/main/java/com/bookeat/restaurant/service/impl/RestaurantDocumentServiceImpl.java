package com.bookeat.restaurant.service.impl;
import com.bookeat.restaurant.dto.RestaurantRequests.*;
import com.bookeat.restaurant.dto.RestaurantResponses.*;
import com.bookeat.restaurant.entity.*;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.repository.*;
import com.bookeat.restaurant.mapper.RestaurantMapper;
import com.bookeat.restaurant.service.*;
import com.bookeat.restaurant.storage.*;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import java.time.Clock;
import java.security.MessageDigest;
import java.util.*;
import java.io.IOException;
@Service @RequiredArgsConstructor @Transactional @Slf4j
public class RestaurantDocumentServiceImpl implements RestaurantDocumentService {
    private final RestaurantDocumentRepository documents;
    private final RestaurantRepository restaurants;
    private final RestaurantAccessService access;
    private final RestaurantChangeService changes;
    private final RestaurantMapper mapper;
    private final OnboardingProgressService progress;
    private final DocumentStorageRouter storage;
    private final Clock clock;
    @Override @PreAuthorize("hasRole('OWNER') and hasAuthority('RESTAURANT_OWN_MANAGE')")
    public Detail upload(UUID actor,UUID id,DocumentUpload q,MultipartFile file) {
        var r=access.lockOwnedRestaurant(actor,id,q.version());
        if(!WorkflowRules.settingsEditable(r.getApprovalStatus())) throw new ApiException(ErrorCode.RESTAURANT_STATE_INVALID);
        byte[] bytes;
        try(var stream=file.getInputStream()) {bytes=stream.readNBytes(DocumentContentRules.MAX_BYTES+1);}
        catch(IOException ex){throw new ApiException(ErrorCode.DOCUMENT_INVALID);}
        String type;
        try{type=DocumentContentRules.detect(bytes);}catch(IllegalArgumentException ex){throw new ApiException(ErrorCode.DOCUMENT_INVALID);}
        if((q.documentType()==DocumentType.LOGO || q.documentType()==DocumentType.COVER_IMAGE)
            && !type.startsWith("image/")) throw new ApiException(ErrorCode.DOCUMENT_INVALID);
        var d=new RestaurantDocument();d.setRestaurant(r);d.setDocumentType(q.documentType());d.setDocumentNumber(q.documentNumber().trim());
        if(!progress.documentMatches(r,d)) throw new ApiException(ErrorCode.VALIDATION_FAILED);
        var current=documents.findByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(id);
        if(current.size()>=20 && (q.documentType()==DocumentType.OTHER || current.stream().noneMatch(e->e.getDocumentType()==q.documentType()))) throw new ApiException(ErrorCode.CONFLICT);
        // Supersede an old document of the same type while retaining its verification history.
        if(q.documentType()!=DocumentType.OTHER) current.stream().filter(e->e.getDocumentType()==q.documentType()).forEach(e->e.setDeletedAt(clock.instant()));
        documents.flush();
        var store=storage.current();String key="restaurants/"+id+"/documents/"+UUID.randomUUID();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCompletion(int status) {
                if(status!=STATUS_COMMITTED) try{store.delete(key);}catch(RuntimeException ex){log.error("Private document rollback cleanup failed; reconcile storage with database metadata");}
            }
        });
        store.put(key,bytes,type);d.setFileKey(key);d.setStorageProvider(store.provider());d.setOriginalFilename(DocumentContentRules.safeFilename(file.getOriginalFilename()));
        d.setContentType(type);d.setSizeBytes(bytes.length);d.setSha256(digest(bytes));d.setVerificationStatus(VerificationStatus.PENDING);
        d.setFileUrl("pending");documents.save(d);
        String privateUrl="/api/v1/owner/restaurants/"+id+"/documents/"+d.getId()+"/download";
        String publicMediaUrl="/api/v1/public/restaurants/"+id+"/media/"+d.getId();
        d.setFileUrl(privateUrl);
        if(q.documentType()==DocumentType.LOGO) r.setLogoUrl(publicMediaUrl);
        if(q.documentType()==DocumentType.COVER_IMAGE) r.setCoverImageUrl(publicMediaUrl);
        changes.finish(r,actor,"RESTAURANT_DOCUMENT_UPLOADED");return mapper.detail(r,false);
    }
    @Override @PreAuthorize("hasRole('OWNER') and hasAuthority('RESTAURANT_OWN_MANAGE')")
    public Detail delete(UUID actor,UUID id,UUID documentId,long version) {
        var r=access.lockOwnedRestaurant(actor,id,version);
        if(!WorkflowRules.settingsEditable(r.getApprovalStatus())) throw new ApiException(ErrorCode.RESTAURANT_STATE_INVALID);
        var d=document(id,documentId);
        d.setDeletedAt(clock.instant());
        if(d.getDocumentType()==DocumentType.LOGO) r.setLogoUrl(null);
        if(d.getDocumentType()==DocumentType.COVER_IMAGE) r.setCoverImageUrl(null);
        changes.finish(r,actor,"RESTAURANT_DOCUMENT_DELETED");return mapper.detail(r,false);
    }
    @Override @Transactional(readOnly=true) @PreAuthorize("hasRole('OWNER') and hasAuthority('RESTAURANT_OWN_MANAGE')")
    public List<DocumentView> listOwner(UUID actor,UUID id){access.getOwnedRestaurant(actor,id);return documents.findByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(id).stream().map(d->mapper.map(d,false)).toList();}
    @Override @Transactional(readOnly=true) @PreAuthorize("hasAuthority('RESTAURANT_DOCUMENT_VERIFY')")
    public List<DocumentView> listAdmin(UUID id){requireRestaurant(id);return documents.findByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(id).stream().map(d->mapper.map(d,true)).toList();}
    @Override @Transactional(readOnly=true) @PreAuthorize("hasRole('OWNER') and hasAuthority('RESTAURANT_OWN_MANAGE')")
    public Download downloadOwner(UUID actor,UUID id,UUID documentId){access.getOwnedRestaurant(actor,id);return download(document(id,documentId));}
    @Override @Transactional(readOnly=true) @PreAuthorize("hasAuthority('RESTAURANT_DOCUMENT_VERIFY')")
    public Download downloadAdmin(UUID id,UUID documentId){requireRestaurant(id);return download(document(id,documentId));}
    private Download download(RestaurantDocument d) {
        byte[] bytes=storage.forProvider(d.getStorageProvider()).read(d.getFileKey());
        if(!digest(bytes).equals(d.getSha256())) throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        return new Download(d.getOriginalFilename(),bytes);
    }
    private String digest(byte[] bytes) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException("SHA-256 unavailable");}
    }
    private RestaurantDocument document(UUID id,UUID documentId){return documents.findByIdAndRestaurantIdAndDeletedAtIsNull(documentId,id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));}
    private Restaurant requireRestaurant(UUID id){return restaurants.findById(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));}
    @Override @PreAuthorize("hasAuthority('RESTAURANT_DOCUMENT_VERIFY')")
    public Detail verify(UUID actor,UUID id,UUID documentId,Verification q) {
        var r=access.lockForReview(id,q.version());
        if(r.getOwner().getUser().getId().equals(actor)) throw new ApiException(ErrorCode.ACCESS_DENIED);
        if(!WorkflowRules.permits(r.getApprovalStatus(),ReviewAction.DOCUMENT_VERIFY)) throw new ApiException(ErrorCode.RESTAURANT_STATE_INVALID);
        if(!q.verified() && (q.reason()==null || q.reason().isBlank())) throw new ApiException(ErrorCode.VALIDATION_FAILED);
        var d=document(id,documentId);
        if(q.verified() && !progress.documentMatches(r,d)) throw new ApiException(ErrorCode.VALIDATION_FAILED);
        d.setVerificationStatus(q.verified()?VerificationStatus.VERIFIED:VerificationStatus.REJECTED);d.setVerifiedBy(actor);d.setVerifiedAt(clock.instant());d.setRejectionReason(q.verified()?null:q.reason());
        changes.history(r,actor,ReviewAction.DOCUMENT_VERIFY,r.getApprovalStatus(),q.reason(),null,d.getId());
        changes.finish(r,actor,"RESTAURANT_DOCUMENT_VERIFIED");return mapper.detail(r,true);
    }
}
