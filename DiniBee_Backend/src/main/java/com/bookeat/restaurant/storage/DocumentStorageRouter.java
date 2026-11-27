package com.bookeat.restaurant.storage;
import com.bookeat.restaurant.enums.StorageProvider;
import com.bookeat.exception.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class DocumentStorageRouter {
    private final Map<StorageProvider,DocumentStorage> stores=new EnumMap<>(StorageProvider.class);
    private final StorageProvider preferred;
    public DocumentStorageRouter(List<DocumentStorage> stores,@Value("${app.storage.provider:LOCAL}") StorageProvider preferred) {
        stores.forEach(s->this.stores.put(s.provider(),s));this.preferred=preferred;
        if(!this.stores.containsKey(preferred)) throw new IllegalArgumentException("Selected document storage provider is unavailable");
    }
    public DocumentStorage current(){return forProvider(preferred);}
    public DocumentStorage forProvider(StorageProvider provider) {
        DocumentStorage s=stores.get(provider);
        if(s==null) throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE); return s;
    }
}
