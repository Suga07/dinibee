package com.bookeat;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.nio.file.*;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/** Uses real JWT registration, permissions, Flyway migrations and PostgreSQL transactions. */
@SpringBootTest(properties={"app.mail.enabled=false","app.docs-public=true","springdoc.api-docs.enabled=true"})
@AutoConfigureMockMvc @Testcontainers
class RestaurantOnboardingIT {
    @Container static final PostgreSQLContainer<?> DB=new PostgreSQLContainer<>("postgres:17-alpine");
    private static final Path STORAGE=tempStorage();
    private static Path tempStorage(){try{return Files.createTempDirectory("bookeat-onboarding-it-");}catch(Exception ex){throw new IllegalStateException(ex);}}
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url",DB::getJdbcUrl);p.add("spring.datasource.username",DB::getUsername);p.add("spring.datasource.password",DB::getPassword);
        p.add("app.auth.jwt-secret",()->Base64.getEncoder().encodeToString(new byte[32]));
        p.add("app.auth.token-pepper",()->Base64.getEncoder().encodeToString(new byte[32]));
        p.add("app.mail.encryption-key",()->Base64.getEncoder().encodeToString(new byte[32]));
        byte[] key=new byte[32];Arrays.fill(key,(byte)5);p.add("app.settlement.encryption-key",()->Base64.getEncoder().encodeToString(key));
        p.add("app.storage.local-root",STORAGE::toString);
    }
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;
    String ownerToken,otherToken,adminToken,id;long version;
    @BeforeEach void setup() throws Exception {
        jdbc.execute("truncate table users, mail_outbox, auth_rate_limits cascade");
        ownerToken=register("owner","owner@example.test");otherToken=register("owner","other@example.test");
        // Tests grant existing seeded roles in the database; production registration cannot choose admin roles.
        adminToken=register("customer","admin@example.test");
        grant("admin@example.test","SUPER_ADMIN");
        capture(call("POST","/owner/restaurants",Map.of("restaurantName","Chennai Kitchen","restaurantType","RESTAURANT","email","restaurant@example.test","phone","+919876543210"),ownerToken).andExpect(status().isCreated()));
    }
    @AfterAll static void cleanupFiles() throws Exception {try(var files=Files.walk(STORAGE)){for(var path:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    void grant(String email,String role){jdbc.update("insert into user_roles(user_id,role_id) select u.id,r.id from users u cross join roles r where u.email=? and r.code=? on conflict do nothing",email,role);}
    String register(String role,String email) throws Exception {
        return data(call("POST","/auth/register/"+role,Map.of("firstName","Test","lastName","User","email",email,"password","Example-passphrase-71"),null).andExpect(status().isCreated())).path("accessToken").asText();
    }
    ResultActions call(String method,String path,Object body,String token) throws Exception {
        var req=switch(method){case "POST"->post("/api/v1"+path);case "PUT"->put("/api/v1"+path);case "PATCH"->patch("/api/v1"+path);case "DELETE"->delete("/api/v1"+path);default->get("/api/v1"+path);};
        if(token!=null)req.header("Authorization","Bearer "+token);
        if(body!=null)req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        return mvc.perform(req);
    }
    JsonNode data(ResultActions a) throws Exception{return json.readTree(a.andReturn().getResponse().getContentAsString()).path("data");}
    JsonNode capture(ResultActions a) throws Exception {var d=data(a);id=d.path("id").asText();version=d.path("version").asLong();return d;}
    String ownerPath(String suffix){return "/owner/restaurants/"+id+suffix;}
    String adminPath(String suffix){return "/admin/restaurants/"+id+suffix;}
    ObjectNode body(Object fields){var b=(ObjectNode)json.valueToTree(fields);b.put("version",version);return b;}
    JsonNode save(String path,Object values) throws Exception {return capture(call("PUT",ownerPath(path),body(values),ownerToken).andExpect(status().isOk()));}
    JsonNode review(String path,Object values) throws Exception {return capture(call("PUT",adminPath(path),body(values),adminToken).andExpect(status().isOk()));}
    JsonNode upload(String type,String number) throws Exception {
        var metadata=new MockMultipartFile("metadata","metadata.json","application/json",json.writeValueAsBytes(body(Map.of("documentType",type,"documentNumber",number))));
        var file=new MockMultipartFile("file","evidence.pdf","application/pdf","%PDF-1.7 test document".getBytes());
        return capture(mvc.perform(multipart("/api/v1"+ownerPath("/documents")).file(metadata).file(file).header("Authorization","Bearer "+ownerToken)).andExpect(status().isOk()));
    }
    void ready() throws Exception {
        save("",Map.of("restaurantName","Chennai Kitchen","restaurantType","RESTAURANT","description","Fresh food and family dining","email","restaurant@example.test","phone","+919876543210"));
        save("/location",Map.of("addressLine1","10 Test Road","city","Chennai","state","Tamil Nadu","country","IN","postalCode","600001","latitude",13.0827,"longitude",80.2707,"timezone","Asia/Kolkata"));
        save("/business-info",Map.of("businessRegistrationNumber","REG-1234","fssaiNumber","12345678901234"));
        var hours=new ArrayList<Map<String,Object>>();for(var day:DayOfWeek.values())hours.add(Map.of("dayOfWeek",day.name(),"openingTime","09:00:00","closingTime","22:00:00","closed",false,"closesNextDay",false));
        save("/operating-hours",Map.of("hours",hours));save("/services",Map.of("services",List.of("RESTAURANT","TAKEAWAY")));
        save("/table-setup",Map.of("plannedTableCount",10,"plannedSeatCount",40));
        var reservation=json.readTree("{\"reservationEnabled\":true,\"minimumGuests\":1,\"maximumGuestsPerBooking\":8,\"slotDurationMinutes\":30,\"reservationDurationMinutes\":90,\"advanceBookingDays\":30,\"minimumAdvanceMinutes\":30,\"gracePeriodMinutes\":15,\"cancellationAllowed\":true,\"cancellationBeforeMinutes\":60,\"autoConfirm\":false,\"specialRequestAllowed\":true,\"preOrderAllowed\":false,\"gpsCheckInEnabled\":true,\"qrCheckInEnabled\":true,\"gpsCheckInRadiusMeters\":100}");
        save("/reservation-settings",reservation);
        save("/takeaway-settings",Map.of("enabled",true,"regularTakeawayEnabled",true,"onTheWayEnabled",false,"minimumOrderAmount",0,"averagePreparationMinutes",20,"maximumAdvanceOrderMinutes",120,"acceptCash",true,"acceptOnlinePayment",false));
        save("/menu-setup",Map.of("mode","CREATE_IN_MENU_MODULE","acknowledged",true));
        save("/settlement-profile",Map.of("accountHolderName","Test Owner","bankName","Test Bank","accountNumber","123456789012","ifscCode","SBIN0001234","taxName","Test Owner"));
        save("/policies",Map.of("cancellationPolicy","Cancel before the configured deadline","refundPolicy","Refund after review"));
        save("/team-access",Map.of("mode","OWNER_ONLY","acknowledged",true,"members",List.of()));
        upload("FSSAI","12345678901234");upload("BUSINESS_REGISTRATION","REG-1234");upload("OWNER_ID","ID-1234");upload("PAN","ABCDE1234F");
    }
    void submit() throws Exception {capture(call("POST",ownerPath("/submit"),Map.of("version",version),ownerToken).andExpect(status().isOk()));}
    void verifiedReview() throws Exception {
        ready();submit();review("/under-review",Map.of());
        var docs=data(call("GET",adminPath("/documents"),null,adminToken).andExpect(status().isOk()));
        for(var doc:docs)review("/documents/"+doc.path("id").asText()+"/verification",Map.of("verified",true));
        review("/settlement-profile/verification",Map.of("verified",true));
    }
    @Test void fullFormSurvivesDatabaseReloadAndCannotAcceptCredentials() throws Exception {
        ready();
        var form=json.readTree("""
            {"website":"https://example.test","cuisineType":"South Indian","businessName":"Test Hospitality",
             "panNumber":"ABCDE1234F","landmark":"Near station","waitlistEnabled":true,"customerTableSelection":false,
             "packagingCharge":12.50,"pickupInstructions":"Side entrance","payoutSchedule":"Weekly",
             "noShowPolicy":"Call owner","lateArrivalPolicy":"15 minutes","takeawayCancellationPolicy":"Before preparation",
             "tables":[{"tableNumber":"A1","capacity":4,"location":"Ground floor","available":true}],
             "menuCategories":[{"name":"Mains","items":[{"name":"Dosa","description":"Rice pancake","price":95,
               "category":"Mains","vegetarian":true,"dineIn":true,"takeaway":true}]}],
             "staff":[{"name":"Test Manager","phone":"12345678","email":"staff@example.test","role":"Manager","permissions":["View Bookings"]}]}
            """);
        save("/onboarding-form",Map.of("form",form));
        var reloaded=data(call("GET",ownerPath(""),null,ownerToken).andExpect(status().isOk()));
        assertThat(reloaded.path("city").asText()).isEqualTo("Chennai");
        assertThat(reloaded.path("businessRegistrationNumber").asText()).isEqualTo("REG-1234");
        assertThat(reloaded.path("operatingHours").size()).isEqualTo(7);
        assertThat(reloaded.path("settlementProfile").path("maskedAccountNumber").asText()).isEqualTo("****9012");
        var stored=json.readTree(jdbc.queryForObject("select onboarding_form::text from restaurants where id=?",String.class,UUID.fromString(id)));
        assertThat(stored.path("businessName").asText()).isEqualTo("Test Hospitality");
        assertThat(stored.path("menuCategories").get(0).path("items").get(0).path("name").asText()).isEqualTo("Dosa");
        assertThat(stored.path("staff").get(0).path("phone").asText()).isEqualTo("12345678");
        assertThat(stored.has("accountNumber")).isFalse();
        assertThat(reloaded.path("onboardingForm").path("tables").get(0).path("tableNumber").asText()).isEqualTo("A1");
        ((ObjectNode)form).put("password","must-not-be-stored");
        call("PUT",ownerPath("/onboarding-form"),body(Map.of("form",form)),ownerToken).andExpect(status().isBadRequest());
        call("PUT",ownerPath("/onboarding-form"),body(Map.of("form",form)),otherToken).andExpect(status().is4xxClientError());
    }
    @Test void draftCannotBeReviewedOrApprovedAsSubmitted() throws Exception {
        call("PUT",adminPath("/under-review"),body(Map.of()),adminToken).andExpect(status().isConflict());
        call("PUT",adminPath("/approve"),body(Map.of("services",List.of("RESTAURANT"))),adminToken).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select submitted_at from restaurants where id=?",Object.class,UUID.fromString(id))).isNull();
    }
    @Test void uploadedLogoIsStoredAndDetailsSaveDoesNotEraseIt() throws Exception {
        byte[] png={(byte)137,80,78,71,13,10,26,10};
        var metadata=new MockMultipartFile("metadata","metadata.json","application/json",json.writeValueAsBytes(body(Map.of("documentType","LOGO","documentNumber","LOGO"))));
        var file=new MockMultipartFile("file","logo.png","image/png",png);
        var uploaded=capture(mvc.perform(multipart("/api/v1"+ownerPath("/documents")).file(metadata).file(file).header("Authorization","Bearer "+ownerToken)).andExpect(status().isOk()));
        String url=uploaded.path("logoUrl").asText();
        assertThat(url).contains("/documents/").endsWith("/download");
        save("",Map.of("restaurantName","Test Kitchen","restaurantType","RESTAURANT","description","Fresh food","email","restaurant@example.test","phone","+919876543210"));
        assertThat(jdbc.queryForObject("select logo_url from restaurants where id=?",String.class,UUID.fromString(id))).isEqualTo(url);
        mvc.perform(get(url).header("Authorization","Bearer "+ownerToken)).andExpect(status().isOk()).andExpect(content().bytes(png));
    }
    @Test void fiveMigrationsAndDraftCreatedWithoutActivation() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success=true and version in ('1','2','3','4','5')",Integer.class)).isEqualTo(5);
        var d=data(call("GET",ownerPath(""),null,ownerToken).andExpect(status().isOk()));
        assertThat(d.path("approvalStatus").asText()).isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("select count(*) from restaurant_service_activations where enabled",Integer.class)).isZero();
    }
    @Test void otherOwnerCannotReadUpdateSubmitOrDownload() throws Exception {
        call("GET",ownerPath(""),null,otherToken).andExpect(status().isNotFound());
        call("PUT",ownerPath("/services"),body(Map.of("services",List.of("RESTAURANT"))),otherToken).andExpect(status().isNotFound());
        call("POST",ownerPath("/submit"),Map.of("version",version),otherToken).andExpect(status().isNotFound());
        call("GET",ownerPath("/documents/"+UUID.randomUUID()+"/download"),null,otherToken).andExpect(status().isNotFound());
    }
    @Test void incompleteSubmitReturnsMissingStepsAndRemainsDraft() throws Exception {
        call("POST",ownerPath("/submit"),Map.of("version",version),ownerToken).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errorCode").value("ONBOARDING_INCOMPLETE")).andExpect(jsonPath("$.errors[0].field").value("RESTAURANT_DETAILS"));
        assertThat(jdbc.queryForObject("select approval_status from restaurants where id=?",String.class,UUID.fromString(id))).isEqualTo("DRAFT");
    }
    @Test void ownerCannotSetApprovalOrActivateServicesThroughJson() throws Exception {
        var q=body(Map.of("services",List.of("RESTAURANT")));q.put("enabled",true);
        call("PUT",ownerPath("/services"),q,ownerToken).andExpect(status().isBadRequest());
        save("/services",Map.of("services",List.of("RESTAURANT")));
        assertThat(data(call("GET","/owner/dashboard-context?restaurantId="+id,null,ownerToken).andExpect(status().isOk())).path("enabledServices").size()).isZero();
    }
    @Test void staleVersionRejectsConcurrentStepOverwrite() throws Exception {
        long old=version;save("/services",Map.of("services",List.of("RESTAURANT")));
        call("PUT",ownerPath("/services"),Map.of("version",old,"services",List.of("TAKEAWAY")),ownerToken).andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("STALE_RESTAURANT_VERSION"));
    }
    @Test void parallelEditsWithSameVersionHaveExactlyOneWinner() throws Exception {
        var q=Map.of("version",version,"services",List.of("RESTAURANT"));var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            Callable<Integer> task=()->{start.await();return call("PUT",ownerPath("/services"),q,ownerToken).andReturn().getResponse().getStatus();};
            var a=pool.submit(task);var b=pool.submit(task);start.countDown();
            assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        } finally {pool.shutdownNow();}
    }
    @Test void invalidTimezoneDuplicateDaysAndCapacityReturnValidationErrors() throws Exception {
        call("PUT",ownerPath("/location"),body(Map.of("addressLine1","Road","city","Chennai","state","TN","country","IN","postalCode","600001","latitude",13,"longitude",80,"timezone","invalid/zone")),ownerToken).andExpect(status().isBadRequest());
        var day=Map.of("dayOfWeek","MONDAY","closed",true,"closesNextDay",false);
        call("PUT",ownerPath("/operating-hours"),body(Map.of("hours",Collections.nCopies(7,day))),ownerToken).andExpect(status().isBadRequest());
        call("PUT",ownerPath("/table-setup"),body(Map.of("plannedTableCount",10,"plannedSeatCount",1)),ownerToken).andExpect(status().isBadRequest());
    }
    @Test void settlementIsEncryptedAtRestAndMaskedInEveryDetail() throws Exception {
        var d=save("/settlement-profile",Map.of("accountHolderName","Owner","bankName","Bank","accountNumber","123456789012","ifscCode","SBIN0001234","taxName","Owner"));
        assertThat(d.path("settlementProfile").path("maskedAccountNumber").asText()).isEqualTo("****9012");
        assertThat(d.toString()).doesNotContain("123456789012","encryptedAccountNumber");
        assertThat(jdbc.queryForObject("select encrypted_account_number from restaurant_settlement_profiles where restaurant_id=?",String.class,UUID.fromString(id))).startsWith("v1:").doesNotContain("123456789012");
    }
    @Test void documentDownloadIsPrivateAndBoundToRestaurant() throws Exception {
        var d=upload("OWNER_ID","ID-1234");var doc=d.path("documents").get(0).path("id").asText();
        call("GET",ownerPath("/documents/"+doc+"/download"),null,null).andExpect(status().isUnauthorized());
        call("GET",ownerPath("/documents/"+doc+"/download"),null,otherToken).andExpect(status().isNotFound());
        call("GET",ownerPath("/documents/"+doc+"/download"),null,ownerToken).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("X-Content-Type-Options","nosniff"));
        assertThat(d.toString()).doesNotContain("fileKey","storageProvider","ID-1234");
    }
    @Test void invalidDocumentBytesAreRejectedWithoutMetadata() throws Exception {
        var metadata=new MockMultipartFile("metadata","metadata.json","application/json",json.writeValueAsBytes(body(Map.of("documentType","OWNER_ID","documentNumber","ID-1234"))));
        mvc.perform(multipart("/api/v1"+ownerPath("/documents")).file(metadata).file(new MockMultipartFile("file","fake.pdf","application/pdf","<script/>".getBytes())).header("Authorization","Bearer "+ownerToken)).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from restaurant_documents",Integer.class)).isZero();
    }
    @Test void documentReplacementKeepsOneCurrentTypeAndResetsVerification() throws Exception {
        upload("OWNER_ID","ID-1234");var d=upload("OWNER_ID","ID-2345");
        assertThat(d.path("documents").size()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from restaurant_documents",Integer.class)).isEqualTo(2);
        assertThat(d.path("documents").get(0).path("verificationStatus").asText()).isEqualTo("PENDING");
    }
    @Test void completeSubmissionLocksOwnerEditsAndAppearsInPendingList() throws Exception {
        ready();submit();
        call("PUT",ownerPath("/services"),body(Map.of("services",List.of("RESTAURANT"))),ownerToken).andExpect(status().isConflict());
        var d=data(call("GET","/admin/restaurants/pending",null,adminToken).andExpect(status().isOk()));
        assertThat(d.path("totalElements").asInt()).isEqualTo(1);
    }
    @Test void approvalRequiresUnderReviewThenVerifiedDocumentsAndSettlement() throws Exception {
        call("PUT",adminPath("/approve"),body(Map.of("services",List.of("RESTAURANT"))),adminToken).andExpect(status().isConflict());
        ready();submit();review("/under-review",Map.of());
        call("PUT",adminPath("/approve"),body(Map.of("services",List.of("RESTAURANT"))),adminToken).andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("VERIFICATION_REQUIRED"));
    }
    @Test void approvalActivatesOnlyExplicitlyApprovedServicesAndWritesAudit() throws Exception {
        verifiedReview();review("/approve",Map.of("services",List.of("RESTAURANT")));
        var d=data(call("GET","/owner/dashboard-context?restaurantId="+id,null,ownerToken).andExpect(status().isOk()));
        assertThat(d.path("approvalStatus").asText()).isEqualTo("APPROVED");
        assertThat(d.path("enabledServices").toString()).isEqualTo("[\"RESTAURANT\"]");
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='RESTAURANT_APPROVE'",Integer.class)).isEqualTo(1);
    }
    @Test void suspensionAndReactivationDoNotRestoreAdminDisabledService() throws Exception {
        verifiedReview();review("/approve",Map.of("services",List.of("RESTAURANT","TAKEAWAY")));
        capture(call("PATCH",adminPath("/services/TAKEAWAY"),body(Map.of("enabled",false,"reason","Pause takeaway")),adminToken).andExpect(status().isOk()));
        review("/suspend",Map.of("reason","Review incident"));
        assertThat(data(call("GET","/owner/dashboard-context?restaurantId="+id,null,ownerToken).andExpect(status().isOk())).path("enabledServices").size()).isZero();
        review("/reactivate",Map.of("reason","Incident resolved"));
        assertThat(data(call("GET","/owner/dashboard-context?restaurantId="+id,null,ownerToken).andExpect(status().isOk())).path("enabledServices").toString()).isEqualTo("[\"RESTAURANT\"]");
    }
    @Test void rejectionNeedsReasonAndCanBeResubmitted() throws Exception {
        ready();submit();call("PUT",adminPath("/reject"),body(Map.of("reason","")),adminToken).andExpect(status().isBadRequest());
        review("/reject",Map.of("reason","Please correct details"));submit();
        assertThat(jdbc.queryForObject("select approval_status from restaurants where id=?",String.class,UUID.fromString(id))).isEqualTo("SUBMITTED");
    }
    @Test void changesRequestedDisablesActiveServicesAndReopensOwnerEditing() throws Exception {
        verifiedReview();review("/approve",Map.of("services",List.of("RESTAURANT")));
        review("/request-changes",Map.of("reason","Update business details"));
        save("/services",Map.of("services",List.of("RESTAURANT")));
        assertThat(jdbc.queryForObject("select count(*) from restaurant_service_activations where enabled",Integer.class)).isZero();
    }
    @Test void permissionsAreFineGrainedAndOwnerCannotApprove() throws Exception {
        call("PUT",adminPath("/approve"),body(Map.of("services",List.of("RESTAURANT"))),ownerToken).andExpect(status().isForbidden());
        var finance=register("customer","finance@example.test");grant("finance@example.test","FINANCE_ADMIN");
        call("GET",adminPath(""),null,finance).andExpect(status().isOk());
        call("GET",adminPath("/documents"),null,finance).andExpect(status().isForbidden());
        call("PUT",adminPath("/approve"),body(Map.of("services",List.of("RESTAURANT"))),finance).andExpect(status().isForbidden());
    }
    @Test void managerWithoutRestaurantAssignmentIsDenied() throws Exception {
        var manager=register("customer","manager@example.test");grant("manager@example.test","RESTAURANT_MANAGER");
        call("GET",ownerPath(""),null,manager).andExpect(status().isForbidden());
    }
    @Test void draftTeamNeverGrantsRolesOrCreatesUsers() throws Exception {
        save("/team-access",Map.of("mode","DRAFT_TEAM","acknowledged",true,"members",List.of(Map.of("email","staff@example.test","role","RESTAURANT_STAFF"))));
        assertThat(jdbc.queryForObject("select count(*) from users where email='staff@example.test'",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from restaurant_team_drafts",Integer.class)).isEqualTo(1);
    }
    @Test void emptyOwnerDashboardHasNoFakeRestaurantOrMetrics() throws Exception {
        var d=data(call("GET","/owner/dashboard-context",null,otherToken).andExpect(status().isOk()));
        assertThat(d.hasNonNull("restaurant")).isFalse();assertThat(d.path("enabledServices").size()).isZero();assertThat(d.path("onboardingComplete").asBoolean()).isFalse();
    }
    @Test void paginationRejectsUnboundedRequests() throws Exception {
        call("GET","/owner/restaurants?size=101",null,ownerToken).andExpect(status().isBadRequest());
        call("GET","/admin/restaurants?page=-1",null,adminToken).andExpect(status().isBadRequest());
    }
    @Test void swaggerIncludesOnboardingAndApproval() throws Exception {
        var paths=json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("paths");
        assertThat(paths.has("/api/v1/owner/restaurants/{id}/submit")).isTrue();assertThat(paths.has("/api/v1/admin/restaurants/{id}/approve")).isTrue();
    }
    @Test void selectedDiniTicketRequiresItsSettings() throws Exception {
        ready();save("/services",Map.of("services",List.of("RESTAURANT","DINITICKET")));
        var p=data(call("GET",ownerPath("/onboarding-progress"),null,ownerToken).andExpect(status().isOk()));
        assertThat(p.path("diniticketSettingsComplete").asBoolean()).isFalse();
        save("/diniticket-settings",Map.of("enabled",true,"maximumQueueSize",40,"defaultEstimatedMinutesPerParty",5,"allowPreOrder",false,"allowRemoteJoin",true,"ticketExpiryMinutes",60,"autoExpireTickets",true));
        submit();
    }
    @Test void adminCannotActivateUnrequestedService() throws Exception {
        verifiedReview();
        call("PUT",adminPath("/approve"),body(Map.of("services",List.of("DINITICKET"))),adminToken).andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SERVICE_NOT_AVAILABLE"));
    }
    @Test void rejectedDocumentBlocksApprovalUntilChangesAreAddressed() throws Exception {
        ready();submit();review("/under-review",Map.of());
        var docs=data(call("GET",adminPath("/documents"),null,adminToken).andExpect(status().isOk()));
        var doc=docs.get(0).path("id").asText();
        review("/documents/"+doc+"/verification",Map.of("verified",false,"reason","Document is unreadable"));
        call("PUT",adminPath("/approve"),body(Map.of("services",List.of("RESTAURANT"))),adminToken).andExpect(status().isUnprocessableEntity());
        review("/request-changes",Map.of("reason","Replace unreadable document"));
        assertThat(data(call("GET",ownerPath("/onboarding-progress"),null,ownerToken).andExpect(status().isOk())).path("businessInfoComplete").asBoolean()).isFalse();
    }
    @Test void reviewerCannotApproveOwnRestaurant() throws Exception {
        grant("owner@example.test","SUPER_ADMIN");
        call("PUT",adminPath("/under-review"),Map.of("version",version),ownerToken).andExpect(status().isForbidden());
    }

}
