package com.bookeat;
import com.bookeat.restaurant.storage.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class LocalDocumentStorageTest {
    @TempDir Path dir;
    @Test void privateFileRoundTripDoesNotOverwriteAndDeletes() {
        var storage=new LocalDocumentStorage(dir.toString());var key="restaurants/"+UUID.randomUUID()+"/documents/"+UUID.randomUUID();byte[] bytes="%PDF-1.7 private".getBytes();
        storage.put(key,bytes,"application/pdf");assertThat(storage.read(key)).isEqualTo(bytes);
        assertThatThrownBy(()->storage.put(key,bytes,"application/pdf")).isInstanceOf(RuntimeException.class);
        storage.delete(key);assertThat(Files.exists(dir.resolve(key))).isFalse();
    }
    @Test void pathTraversalCannotReadOutsidePrivateDirectory() {
        var storage=new LocalDocumentStorage(dir.toString());
        assertThatThrownBy(()->storage.read("../../secret")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->storage.delete("/tmp/secret")).isInstanceOf(IllegalArgumentException.class);
    }
}
