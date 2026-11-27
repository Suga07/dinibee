package com.bookeat.config;
import com.bookeat.admin.service.AdminBootstrapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component @ConditionalOnProperty(name="app.bootstrap-admin.enabled", havingValue="true")
public class AdminBootstrapRunner implements ApplicationRunner {
    private final AdminBootstrapService service;
    private final String email;
    private final String password;
    public AdminBootstrapRunner(AdminBootstrapService service, @Value("${app.bootstrap-admin.email}") String email,
            @Value("${app.bootstrap-admin.password}") String password) {
        this.service=service; this.email=email; this.password=password;
    }
    @Override public void run(ApplicationArguments args) { service.createFirstAdmin(email, password); }
}
