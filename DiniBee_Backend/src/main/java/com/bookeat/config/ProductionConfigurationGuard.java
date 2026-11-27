package com.bookeat.config;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.*;
import org.springframework.stereotype.Component;
import java.net.URI;

@Component
public class ProductionConfigurationGuard implements SmartInitializingSingleton {
    private final Environment env;
    private final AuthProperties auth;
    public ProductionConfigurationGuard(Environment env, AuthProperties auth) { this.env=env; this.auth=auth; }
    @Override public void afterSingletonsInstantiated() {
        if (!env.acceptsProfiles(Profiles.of("prod"))) return;
        if (env.acceptsProfiles(Profiles.of("dev")) || env.getProperty("app.docs-public", Boolean.class, false))
            throw new IllegalStateException("Production cannot enable development or public API documentation");
        if (!env.getProperty("app.mail.enabled", Boolean.class, true))
            throw new IllegalStateException("Production requires verification mail delivery");
        String mailKey=env.getRequiredProperty("app.mail.encryption-key");
        if (auth.jwtSecret().equals(auth.tokenPepper()) || auth.jwtSecret().equals(mailKey) || auth.tokenPepper().equals(mailKey))
            throw new IllegalStateException("Use three independent authentication and encryption keys");
        String settlementKey=env.getProperty("app.settlement.encryption-key", "");
        if(settlementKey.isBlank() || settlementKey.equals(mailKey) || settlementKey.equals(auth.jwtSecret()) || settlementKey.equals(auth.tokenPepper()))
            throw new IllegalStateException("Production requires an independent settlement encryption key");
        if(!"S3".equals(env.getProperty("app.storage.provider")) || !env.getProperty("app.storage.s3.enabled",Boolean.class,false))
            throw new IllegalStateException("Production requires private S3 document storage");
        String origins=env.getRequiredProperty("app.cors.allowed-origins");
        if (origins.isBlank()) throw new IllegalStateException("Configure the production admin HTTPS origin");
        for (String origin : origins.split(",")) {
            URI uri=URI.create(origin.trim());
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || origin.contains("*"))
                throw new IllegalStateException("Production CORS origins must be explicit HTTPS origins");
        }
    }
}
