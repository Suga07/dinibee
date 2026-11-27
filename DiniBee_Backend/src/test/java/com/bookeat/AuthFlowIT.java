package com.bookeat;

import com.bookeat.admin.service.AdminBootstrapService;
import com.bookeat.auth.mail.*;
import com.bookeat.auth.request.RegisterRequest;
import com.bookeat.auth.service.AuthService;
import com.bookeat.common.security.SecretTokens;
import com.bookeat.exception.ApiException;
import com.bookeat.user.entity.RoleCode;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.mail.enabled=false","app.docs-public=true","springdoc.api-docs.enabled=true"})
@AutoConfigureMockMvc @Testcontainers
class AuthFlowIT {
    @Container static final PostgreSQLContainer<?> DB=new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", DB::getJdbcUrl);
        properties.add("spring.datasource.username", DB::getUsername);
        properties.add("spring.datasource.password", DB::getPassword);
        properties.add("app.auth.jwt-secret", () -> Base64.getEncoder().encodeToString(new byte[32]));
        properties.add("app.auth.token-pepper", () -> Base64.getEncoder().encodeToString(new byte[32]));
        properties.add("app.mail.encryption-key", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired MailOutboxRepository outboxRepository;
    @Autowired MailOutboxService outbox;
    @Autowired AdminBootstrapService bootstrap;
    @Autowired AuthService auth;
    private static final String PASSWORD="Example-passphrase-71";
    private static final String NEW_PASSWORD="New-example-passphrase-92";

    @BeforeEach void clean() {
        jdbc.execute("truncate table users, mail_outbox, auth_rate_limits cascade");
    }
    private ResultActions postJson(String path, Object body, String token) throws Exception {
        var request=post("/api/v1"+path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if(token!=null) request.header("Authorization","Bearer "+token);
        return mvc.perform(request);
    }
    private JsonNode body(ResultActions actions) throws Exception { return json.readTree(actions.andReturn().getResponse().getContentAsString()); }
    private JsonNode register(String role, String email) throws Exception {
        return body(postJson("/auth/register/"+role,Map.of("firstName","Test","lastName","User","email",email,"password",PASSWORD),null)
                .andExpect(status().isCreated())).path("data");
    }
    private ResultActions me(String token) throws Exception { return mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+token)); }
    private String access(JsonNode tokens) { return tokens.path("accessToken").asText(); }
    private String refresh(JsonNode tokens) { return tokens.path("refreshToken").asText(); }
    private String extract(String text,String label) {
        var matcher=Pattern.compile(Pattern.quote(label)+"([^\\s]+)").matcher(text);
        assertThat(matcher.find()).isTrue(); return matcher.group(1);
    }
    private String mailBody() { return outbox.decrypt(outboxRepository.findAll().get(0)); }

    @Test void healthUsesDatabaseAndMigrationsRun() throws Exception {
        mvc.perform(get("/api/v1/public/health")).andExpect(status().isOk()).andExpect(jsonPath("$.data.database").value("UP"));
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success=true and version in ('1','2','3')",Integer.class)).isEqualTo(3);
    }
    @Test void registrationPersistsSeparateProfileAndHashesSecrets() throws Exception {
        var tokens=register("customer","customer@example.test");
        assertThat(tokens.path("user").path("roles").toString()).contains("CUSTOMER").doesNotContain("ADMIN");
        assertThat(tokens.path("user").has("passwordHash")).isFalse();
        assertThat(jdbc.queryForObject("select password_hash from users",String.class)).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(jdbc.queryForObject("select token_hash from refresh_tokens",String.class)).isEqualTo(SecretTokens.sha256(refresh(tokens)));
        mvc.perform(get("/api/v1/customer/profile").header("Authorization","Bearer "+access(tokens)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.profileId").exists());
        assertThat(jdbc.queryForObject("select count(*) from customers",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from restaurant_owners",Integer.class)).isZero();
    }
    @Test void duplicateEmailIsCaseInsensitive() throws Exception {
        register("customer","customer@example.test");
        postJson("/auth/register/owner",Map.of("firstName","Other","lastName","User","email","CUSTOMER@EXAMPLE.TEST","password",PASSWORD),null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"));
    }
    @Test void invalidLoginAndUnknownAccountAreUnauthorized() throws Exception {
        register("customer","customer@example.test");
        for(String email:List.of("customer@example.test","unknown@example.test"))
            postJson("/auth/login",Map.of("email",email,"password","Wrong-passphrase-22"),null).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"));
    }
    @Test void anonymousAndWrongRoleCannotAccessProtectedProfiles() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        var tokens=register("customer","customer@example.test");
        for(String path:List.of("/api/v1/owner/profile","/api/v1/admin/profile"))
            mvc.perform(get(path).header("Authorization","Bearer "+access(tokens))).andExpect(status().isForbidden());
    }
    @Test void ownerGetsOnlyItsOwnProfile() throws Exception {
        var first=register("owner","first@example.test"); var second=register("owner","second@example.test");
        mvc.perform(get("/api/v1/owner/profile").param("userId",second.path("user").path("id").asText())
                .header("Authorization","Bearer "+access(first))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("first@example.test"));
        mvc.perform(get("/api/v1/customer/profile").header("Authorization","Bearer "+access(first))).andExpect(status().isForbidden());
    }
    @Test void registrationCannotInjectAdminRoleOrExtraFields() throws Exception {
        postJson("/auth/register/customer",Map.of("firstName","Test","lastName","User","email","customer@example.test","password",PASSWORD,"roles",List.of("SUPER_ADMIN")),null)
                .andExpect(status().isBadRequest());
        assertThatThrownBy(() -> auth.register(new RegisterRequest("Test","User","customer@example.test",PASSWORD,null),RoleCode.ADMIN,null)).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("select count(*) from users",Integer.class)).isZero();
    }
    @Test void logoutRevokesCurrentSessionButKeepsOtherDevice() throws Exception {
        var first=register("customer","customer@example.test");
        var other=body(postJson("/auth/login",Map.of("email","customer@example.test","password",PASSWORD),null)).path("data");
        postJson("/auth/logout",Map.of(),access(first)).andExpect(status().isOk());
        me(access(first)).andExpect(status().isUnauthorized()); me(access(other)).andExpect(status().isOk());
        postJson("/auth/refresh-token",Map.of("refreshToken",refresh(first)),null).andExpect(status().isUnauthorized());
    }
    @Test void refreshReuseRevokesSuccessorTokens() throws Exception {
        var first=register("customer","customer@example.test");
        var second=body(postJson("/auth/refresh-token",Map.of("refreshToken",refresh(first)),null).andExpect(status().isOk())).path("data");
        assertThat(refresh(first)).isNotEqualTo(refresh(second));
        postJson("/auth/refresh-token",Map.of("refreshToken",refresh(first)),null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_REFRESH_REUSED"));
        me(access(second)).andExpect(status().isUnauthorized());
        postJson("/auth/refresh-token",Map.of("refreshToken",refresh(second)),null).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from auth_sessions where revoked_at is not null",Integer.class)).isEqualTo(1);
    }
    @Test void simultaneousRefreshHasOneSuccessAndRevokesReusedSession() throws Exception {
        var first=register("customer","customer@example.test");
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            Callable<MvcResult> request=() -> { start.await(); return postJson("/auth/refresh-token",Map.of("refreshToken",refresh(first)),null).andReturn(); };
            var a=pool.submit(request); var b=pool.submit(request); start.countDown();
            var results=List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
            assertThat(results.stream().map(r -> r.getResponse().getStatus()).toList()).containsExactlyInAnyOrder(200,401);
            var success=results.stream().filter(r -> r.getResponse().getStatus()==200).findFirst().orElseThrow();
            me(access(json.readTree(success.getResponse().getContentAsString()).path("data"))).andExpect(status().isUnauthorized());
        } finally { pool.shutdownNow(); }
    }
    @Test void passwordChangeRevokesEverySession() throws Exception {
        var first=register("customer","customer@example.test");
        postJson("/auth/change-password",Map.of("currentPassword",PASSWORD,"newPassword",NEW_PASSWORD),access(first)).andExpect(status().isOk());
        me(access(first)).andExpect(status().isUnauthorized());
        postJson("/auth/refresh-token",Map.of("refreshToken",refresh(first)),null).andExpect(status().isUnauthorized());
        postJson("/auth/login",Map.of("email","customer@example.test","password",NEW_PASSWORD),null).andExpect(status().isOk());
    }
    @Test void resetConsumesTokenAndRevokesSessions() throws Exception {
        var first=register("customer","customer@example.test");
        postJson("/auth/forgot-password",Map.of("email","customer@example.test"),null).andExpect(status().isAccepted()).andExpect(jsonPath("$.data").doesNotExist());
        String mail=mailBody();
        var payload=Map.of("challengeId",extract(mail,"Challenge ID: "),"token",extract(mail,"Reset token: "),"newPassword",NEW_PASSWORD);
        assertThat(jdbc.queryForObject("select encrypted_body from mail_outbox",String.class)).doesNotContain(payload.get("token"));
        postJson("/auth/reset-password",payload,null).andExpect(status().isOk());
        postJson("/auth/reset-password",payload,null).andExpect(status().isBadRequest());
        me(access(first)).andExpect(status().isUnauthorized());
        postJson("/auth/login",Map.of("email","customer@example.test","password",NEW_PASSWORD),null).andExpect(status().isOk());
    }
    @Test void unknownResetEmailGetsSameGenericAcceptedResponse() throws Exception {
        postJson("/auth/forgot-password",Map.of("email","unknown@example.test"),null).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("If the account is eligible, password reset instructions will be emailed"));
        assertThat(outboxRepository.count()).isZero();
    }
    @Test void emailOtpIsSingleUse() throws Exception {
        var tokens=register("customer","customer@example.test");
        var challenge=body(postJson("/auth/send-email-otp",Map.of(),access(tokens)).andExpect(status().isAccepted())).path("data");
        var payload=Map.of("challengeId",challenge.path("challengeId").asText(),"code",extract(mailBody(),"code is: "));
        postJson("/auth/verify-email-otp",payload,access(tokens)).andExpect(status().isOk());
        postJson("/auth/verify-email-otp",payload,access(tokens)).andExpect(status().isBadRequest());
        me(access(tokens)).andExpect(jsonPath("$.data.emailVerified").value(true));
    }
    @Test void invalidOtpAttemptsCommitAndLockAfterFive() throws Exception {
        var tokens=register("customer","customer@example.test");
        var challenge=body(postJson("/auth/send-email-otp",Map.of(),access(tokens))).path("data");
        String correct=extract(mailBody(),"code is: "); String wrong=correct.equals("000000") ? "111111" : "000000";
        for(int i=0;i<5;i++) postJson("/auth/verify-email-otp",Map.of("challengeId",challenge.path("challengeId").asText(),"code",wrong),access(tokens)).andExpect(status().isBadRequest());
        postJson("/auth/verify-email-otp",Map.of("challengeId",challenge.path("challengeId").asText(),"code",correct),access(tokens)).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select attempts from auth_challenges",Integer.class)).isEqualTo(5);
    }
    @Test void customerCannotUseAnotherCustomersOtp() throws Exception {
        var a=register("customer","a@example.test"); var b=register("customer","b@example.test");
        var challenge=body(postJson("/auth/send-email-otp",Map.of(),access(b))).path("data");
        postJson("/auth/verify-email-otp",Map.of("challengeId",challenge.path("challengeId").asText(),"code",extract(mailBody(),"code is: ")),access(a))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select attempts from auth_challenges",Integer.class)).isZero();
    }
    @Test void blockedUserCannotUseExistingAccessToken() throws Exception {
        var tokens=register("customer","customer@example.test"); jdbc.update("update users set status='BLOCKED'");
        me(access(tokens)).andExpect(status().isUnauthorized());
    }
    @Test void removingRoleTakesEffectWithoutWaitingForJwtExpiry() throws Exception {
        var tokens=register("customer","customer@example.test"); jdbc.update("delete from user_roles");
        mvc.perform(get("/api/v1/customer/profile").header("Authorization","Bearer "+access(tokens))).andExpect(status().isForbidden());
    }
    @Test void loginRateLimitPersistsAfterFailedLoginTransactions() throws Exception {
        for(int i=0;i<10;i++) postJson("/auth/login",Map.of("email","unknown@example.test","password",PASSWORD),null).andExpect(status().isUnauthorized());
        postJson("/auth/login",Map.of("email","unknown@example.test","password",PASSWORD),null).andExpect(status().isTooManyRequests());
    }
    @Test void bootstrapCreatesAnAuditedAdminButNeverPromotesAnExistingUser() throws Exception {
        bootstrap.createFirstAdmin("admin@example.test","Admin-example-passphrase-725");
        var admin=body(postJson("/auth/login",Map.of("email","admin@example.test","password","Admin-example-passphrase-725"),null)).path("data");
        mvc.perform(get("/api/v1/admin/profile").header("Authorization","Bearer "+access(admin))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='SUPER_ADMIN_BOOTSTRAPPED'",Integer.class)).isEqualTo(1);
        bootstrap.createFirstAdmin("other-admin@example.test","Admin-example-passphrase-726");
        assertThat(jdbc.queryForObject("select count(*) from users",Integer.class)).isEqualTo(1);
    }
    @Test void swaggerDescribesLiveControllers() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists());
    }
}
