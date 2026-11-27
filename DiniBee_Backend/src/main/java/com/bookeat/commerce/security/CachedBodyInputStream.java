package com.bookeat.commerce.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.ByteArrayInputStream;

/**
 * {@link ServletInputStream} backed by an in-memory byte array.
 *
 * <p>Kept as a separate top-level class (not an inner/anonymous class inside
 * {@code FinalPhaseFilter}) so that incremental / hot-reload compilers always
 * produce a stable, independent {@code .class} file and never throw
 * {@code ClassNotFoundException} at runtime.
 */
class CachedBodyInputStream extends ServletInputStream {

    private final ByteArrayInputStream delegate;

    CachedBodyInputStream(byte[] body) {
        this.delegate = new ByteArrayInputStream(body);
    }

    @Override
    public int read() { return delegate.read(); }

    @Override
    public boolean isFinished() { return delegate.available() == 0; }

    @Override
    public boolean isReady() { return true; }

    @Override
    public void setReadListener(ReadListener listener) {
        throw new UnsupportedOperationException("Synchronous JSON input — ReadListener not supported");
    }
}
