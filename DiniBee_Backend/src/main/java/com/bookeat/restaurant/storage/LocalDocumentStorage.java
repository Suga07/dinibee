package com.bookeat.restaurant.storage;
import com.bookeat.restaurant.enums.StorageProvider;
import com.bookeat.exception.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.io.IOException;
@Component
public class LocalDocumentStorage implements DocumentStorage {
    private final Path root;
    public LocalDocumentStorage(@Value("${app.storage.local-root:./private-documents}") String root) {this.root=Path.of(root).toAbsolutePath().normalize();}
    @Override public StorageProvider provider(){return StorageProvider.LOCAL;}
    private Path path(String key) {
        DocumentContentRules.validateKey(key);
        Path p=root.resolve(key).normalize();
        if(!p.startsWith(root)) throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        // The directory must be owned exclusively by the service user. Reject existing symlink components.
        for(Path c=p;c!=null;c=c.getParent()) if(Files.isSymbolicLink(c)) throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        return p;
    }
    @Override public void put(String key,byte[] content,String contentType) {
        Path target=path(key);
        try {
            Files.createDirectories(target.getParent());
            if(Files.getFileStore(target.getParent()).supportsFileAttributeView("posix")) {
                for(Path p=target.getParent();p!=null && p.startsWith(root);p=p.getParent())
                    Files.setPosixFilePermissions(p,java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
                Files.createFile(target,java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
                Files.write(target,content,StandardOpenOption.WRITE);
            } else Files.write(target,content,StandardOpenOption.CREATE_NEW);
        }
        catch(IOException ex){throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);}
    }
    @Override public byte[] read(String key) {
        Path p=path(key);
        try {
            if(Files.size(p)>DocumentContentRules.MAX_BYTES) throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
            return Files.readAllBytes(p);
        } catch(IOException ex){throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);}
    }
    @Override public void delete(String key) {
        try {Files.deleteIfExists(path(key));} catch(IOException ex){throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);}
    }
}
