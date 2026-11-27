package com.bookeat.restaurant.storage;
import com.bookeat.restaurant.enums.StorageProvider;
public interface DocumentStorage {
    StorageProvider provider();
    void put(String key,byte[] content,String contentType);
    byte[] read(String key);
    void delete(String key);
}
