package com.bookeat.commerce.security;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Wraps an {@link HttpServletRequest} so that a pre-read body byte array can be
 * replayed by downstream filters / controllers.
 *
 * <p>Kept as a separate top-level class (not an inner/anonymous class inside
 * {@code FinalPhaseFilter}) so that incremental / hot-reload compilers always
 * produce a stable, independent {@code .class} file and never throw
 * {@code ClassNotFoundException} at runtime.
 */
class BodyCachingRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    BodyCachingRequestWrapper(HttpServletRequest request, byte[] body) {
        super(request);
        this.body = body;
    }

    @Override
    public int getContentLength() { return body.length; }

    @Override
    public long getContentLengthLong() { return body.length; }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyInputStream(body);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
