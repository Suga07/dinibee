package com.bookeat.restaurant.storage;
import com.bookeat.restaurant.enums.StorageProvider;
import com.bookeat.exception.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import java.time.Duration;
@Component @ConditionalOnProperty(name="app.storage.s3.enabled",havingValue="true")
public class S3DocumentStorage implements DocumentStorage {
    private final S3Client client;
    private final String bucket;
    public S3DocumentStorage(@Value("${app.storage.s3.bucket}") String bucket,@Value("${app.storage.s3.region}") String region) {
        if(bucket.isBlank() || region.isBlank()) throw new IllegalArgumentException("Configure private S3 bucket and region");
        this.bucket=bucket;
        client=S3Client.builder().region(Region.of(region))
            .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(5)).socketTimeout(Duration.ofSeconds(20)))
            .overrideConfiguration(c->c.apiCallTimeout(Duration.ofSeconds(30)).apiCallAttemptTimeout(Duration.ofSeconds(20)))
            .build(); // Default credential provider chain; no hardcoded credentials or public ACL.
    }
    @Override public StorageProvider provider(){return StorageProvider.S3;}
    @Override public void put(String key,byte[] content,String type) {
        DocumentContentRules.validateKey(key);
        try {client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(type)
                .serverSideEncryption(ServerSideEncryption.AES256).build(),RequestBody.fromBytes(content));}
        catch(RuntimeException ex){throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);}
    }
    @Override public byte[] read(String key) {
        DocumentContentRules.validateKey(key);
        try (var response=client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            byte[] bytes=response.readNBytes(DocumentContentRules.MAX_BYTES+1);
            if(bytes.length>DocumentContentRules.MAX_BYTES) throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
            return bytes;
        } catch(Exception ex){throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);}
    }
    @Override public void delete(String key) {
        DocumentContentRules.validateKey(key);
        try {client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());}
        catch(RuntimeException ex){throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);}
    }
    @PreDestroy public void close(){client.close();}
}
