package com.bookeat;
import com.bookeat.availability.service.TableAllocationService;
import com.bookeat.exception.ApiException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/** Real HTTP/JWT and PostgreSQL tests. Only the approved restaurant fixture uses direct SQL;
 * the complete approval workflow is exercised separately by RestaurantOnboardingIT. */
@SpringBootTest(properties={"app.mail.enabled=false"})
@AutoConfigureMockMvc @Testcontainers
class TableMenuAvailabilityIT {
    @Container static final PostgreSQLContainer<?> DB=new PostgreSQLContainer<>("postgres:17-alpine");
    private static final Path STORAGE=tempStorage();
    static Path tempStorage(){try{return Files.createTempDirectory("bookeat-phase4-it-");}catch(Exception e){throw new IllegalStateException(e);}}
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p){
        p.add("spring.datasource.url",DB::getJdbcUrl);p.add("spring.datasource.username",DB::getUsername);p.add("spring.datasource.password",DB::getPassword);
        p.add("app.auth.jwt-secret",()->Base64.getEncoder().encodeToString(new byte[32]));p.add("app.auth.token-pepper",()->Base64.getEncoder().encodeToString(new byte[32]));p.add("app.mail.encryption-key",()->Base64.getEncoder().encodeToString(new byte[32]));
        byte[] key=new byte[32];Arrays.fill(key,(byte)9);p.add("app.settlement.encryption-key",()->Base64.getEncoder().encodeToString(key));p.add("app.storage.local-root",STORAGE::toString);
    }
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;
    @Autowired TableAllocationService allocations;@Autowired PlatformTransactionManager transactions;
    String owner,other,restaurant;UUID actor;long version;LocalDate date;
    @BeforeEach void setup() throws Exception {
        jdbc.execute("truncate table users, mail_outbox, auth_rate_limits cascade");
        owner=register("owner","owner@example.test");other=register("owner","other@example.test");
        actor=jdbc.queryForObject("select id from users where email='owner@example.test'",UUID.class);
        var r=data(call("POST","/owner/restaurants",Map.of("restaurantName","Phase Four Kitchen","restaurantType","RESTAURANT","email","restaurant@example.test","phone","+919876543210"),owner).andExpect(status().isCreated()));
        restaurant=r.path("id").asText();version=r.path("version").asLong();date=LocalDate.now(ZoneOffset.UTC).plusDays(1);
    }
    @AfterAll static void clean() throws Exception {try(var paths=Files.walk(STORAGE)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    String register(String role,String email) throws Exception{return data(call("POST","/auth/register/"+role,Map.of("firstName","Test","lastName","User","email",email,"password","Example-passphrase-71"),null).andExpect(status().isCreated())).path("accessToken").asText();}
    ResultActions call(String method,String path,Object body,String token) throws Exception {
        var req=switch(method){case "POST"->post("/api/v1"+path);case "PUT"->put("/api/v1"+path);case "PATCH"->patch("/api/v1"+path);case "DELETE"->delete("/api/v1"+path);default->get("/api/v1"+path);};
        req.servletPath("/api/v1"+path.split("\\?",2)[0]);
        if(token!=null)req.header("Authorization","Bearer "+token);if(body!=null)req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));return mvc.perform(req);
    }
    JsonNode data(ResultActions a) throws Exception{return json.readTree(a.andReturn().getResponse().getContentAsString()).path("data");}
    JsonNode capture(ResultActions a) throws Exception{var d=data(a);version=d.path("restaurantVersion").asLong();return d.path("resource");}
    String path(String suffix){return "/owner/restaurants/"+restaurant+suffix;}
    ObjectNode body(Object value){var n=(ObjectNode)json.valueToTree(value);n.put("restaurantVersion",version);return n;}
    ObjectNode table(String number){return body(json.convertValue(Map.ofEntries(Map.entry("tableNumber",number),Map.entry("tableName","Window"),Map.entry("capacity",4),Map.entry("minimumGuests",1),Map.entry("maximumGuests",4),Map.entry("floor",0),Map.entry("section","Main"),Map.entry("shape","RECTANGLE"),Map.entry("xPosition",10),Map.entry("yPosition",20),Map.entry("width",100),Map.entry("height",80),Map.entry("rotation",0),Map.entry("active",true)),ObjectNode.class));}
    JsonNode createTable(String number) throws Exception{return capture(call("POST",path("/tables"),table(number),owner).andExpect(status().isCreated()));}
    JsonNode category(String name) throws Exception{return capture(call("POST",path("/menu/categories"),body(Map.of("name",name,"description","Fresh dishes","displayOrder",0,"active",true)),owner).andExpect(status().isCreated()));}
    ObjectNode item(String category,String name){var n=body(Map.ofEntries(Map.entry("categoryId",category),Map.entry("name",name),Map.entry("description","Freshly made"),Map.entry("basePrice",100),Map.entry("discountPrice",80),Map.entry("foodType","VEG"),Map.entry("spiceLevel","MILD"),Map.entry("available",true),Map.entry("preOrderAvailable",true),Map.entry("takeawayAvailable",true),Map.entry("diniTicketPreOrderAvailable",false),Map.entry("preparationTimeMinutes",20),Map.entry("featured",false),Map.entry("displayOrder",0),Map.entry("variants",List.of(Map.of("name","Large","price",150,"active",true))),Map.entry("addons",List.of(Map.of("name","Cheese","price",10,"active",true)))));return n;}
    JsonNode createItem(String category,String name) throws Exception{return capture(call("POST",path("/menu/items"),item(category,name),owner).andExpect(status().isCreated()));}
    void config(String suffix,Object value) throws Exception{var n=(ObjectNode)json.valueToTree(value);n.put("version",version);version=data(call("PUT",path(suffix),n,owner).andExpect(status().isOk())).path("version").asLong();}
    void approved() throws Exception {
        config("/location",Map.of("addressLine1","Test Road","city","Chennai","state","TN","country","IN","postalCode","600001","latitude",13,"longitude",80,"timezone","UTC"));
        var hours=new ArrayList<Map<String,Object>>();for(var d:DayOfWeek.values())hours.add(Map.of("dayOfWeek",d.name(),"openingTime","09:00","closingTime","22:00","closed",false,"closesNextDay",false));config("/operating-hours",Map.of("hours",hours));
        config("/services",Map.of("services",List.of("RESTAURANT")));
        config("/reservation-settings",Map.ofEntries(Map.entry("reservationEnabled",true),Map.entry("minimumGuests",1),Map.entry("maximumGuestsPerBooking",8),Map.entry("slotDurationMinutes",30),Map.entry("reservationDurationMinutes",60),Map.entry("advanceBookingDays",30),Map.entry("minimumAdvanceMinutes",30),Map.entry("gracePeriodMinutes",15),Map.entry("cancellationAllowed",true),Map.entry("cancellationBeforeMinutes",60),Map.entry("autoConfirm",false),Map.entry("specialRequestAllowed",true),Map.entry("preOrderAllowed",true),Map.entry("gpsCheckInEnabled",true),Map.entry("qrCheckInEnabled",true),Map.entry("gpsCheckInRadiusMeters",100)));
        jdbc.update("update restaurants set approval_status='APPROVED',status='ACTIVE',approved_at=now(),approved_by=? where id=?",actor,UUID.fromString(restaurant));
        jdbc.update("update restaurant_service_activations set requested=true,authorized=true,enabled=true where restaurant_id=? and service_type='RESTAURANT'",UUID.fromString(restaurant));
    }
    String availability(String time){return "/restaurants/"+restaurant+"/availability?date="+date+"&guests=2"+(time==null?"":"&time="+time);}
    OffsetDateTime starts(){return date.atTime(12,0).atOffset(ZoneOffset.UTC);}
    TableAllocationService.Claim claim(UUID table,UUID reference){return new TransactionTemplate(transactions).execute(s->allocations.claimReservation(actor,UUID.fromString(restaurant),table,reference,starts(),2));}
    void raw(UUID table,OffsetDateTime start,OffsetDateTime end){jdbc.update("insert into table_allocations(id,restaurant_id,table_id,reference_id,reference_type,starts_at,dining_ends_at,ends_at,guest_count,active) values (?,?,?,?,'RESERVATION',?,?,?,2,true)",UUID.randomUUID(),UUID.fromString(restaurant),table,UUID.randomUUID(),start,end,end);}
    @Test void eightMigrationsAndOwnerPermissions() {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success=true and version in ('1','2','3','4','5','6','7','8')",Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("select count(*) from role_permissions rp join roles r on r.id=rp.role_id join permissions p on p.id=rp.permission_id where r.code='OWNER' and p.code in ('TABLE_MANAGE','MENU_MANAGE')",Integer.class)).isEqualTo(2);
    }
    @Test void tableCrudNormalizesAndArchives() throws Exception {
        var t=createTable("t-01");var id=t.path("id").asText();assertThat(t.path("tableNumber").asText()).isEqualTo("T-01");assertThat(t.path("status").asText()).isEqualTo("AVAILABLE");
        call("GET",path("/tables?page=0&size=10"),null,owner).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        var q=table("T-01");q.put("tableName","Patio");capture(call("PUT",path("/tables/"+id),q,owner).andExpect(status().isOk()));
        capture(call("DELETE",path("/tables/"+id+"?restaurantVersion="+version),null,owner).andExpect(status().isOk()));
        call("GET",path("/tables/"+id),null,owner).andExpect(status().isNotFound());createTable("t-01");
        assertThat(jdbc.queryForObject("select count(*) from restaurant_tables",Integer.class)).isEqualTo(2);
    }
    @Test void tableDuplicateCapacityGeometryAndStateInjectionRejected() throws Exception {
        createTable("T-01");call("POST",path("/tables"),table("t-01"),owner).andExpect(status().isConflict());
        var q=table("T-02");q.put("maximumGuests",5);call("POST",path("/tables"),q,owner).andExpect(status().isBadRequest());
        q=table("T-02");q.put("width",0);call("POST",path("/tables"),q,owner).andExpect(status().isBadRequest());
        q=table("T-02");q.put("status","AVAILABLE");call("POST",path("/tables"),q,owner).andExpect(status().isBadRequest());
    }
    @Test void tableOwnershipAndRolesFailClosed() throws Exception {
        var t=createTable("A");var suffix="/tables/"+t.path("id").asText();
        call("GET",path(suffix),null,other).andExpect(status().isNotFound());call("PUT",path(suffix),table("A"),other).andExpect(status().isNotFound());
        call("DELETE",path(suffix+"?restaurantVersion="+version),null,other).andExpect(status().isNotFound());
        call("GET",path("/tables"),null,null).andExpect(status().isUnauthorized());
        String customer=register("customer","customer@example.test");call("GET",path("/tables"),null,customer).andExpect(status().isForbidden());
    }
    @Test void tableStatusAuditedAndRequiresReason() throws Exception {
        var t=createTable("A");var suffix="/tables/"+t.path("id").asText()+"/status";
        call("PATCH",path(suffix),body(Map.of("status","BLOCKED","reason","")),owner).andExpect(status().isBadRequest());
        capture(call("PATCH",path(suffix),body(Map.of("status","BLOCKED","reason","Private event")),owner).andExpect(status().isOk()));
        assertThat(jdbc.queryForObject("select new_value from audit_logs where action='TABLE_STATUS_CHANGED'",String.class)).contains("Private event","BLOCKED");
    }
    @Test void staleVersionAndReviewLockRejectInventoryMutation() throws Exception {
        long old=version;createTable("A");var q=table("B");q.put("restaurantVersion",old);call("POST",path("/tables"),q,owner).andExpect(status().isConflict());
        jdbc.update("update restaurants set approval_status='UNDER_REVIEW' where id=?",UUID.fromString(restaurant));
        call("POST",path("/tables"),table("B"),owner).andExpect(status().isConflict());
        call("POST",path("/menu/categories"),body(Map.of("name","Locked","displayOrder",0,"active",true)),owner).andExpect(status().isConflict());
    }
    @Test void menuCrudAndCategoryDeletionGuard() throws Exception {
        String c=category("Main").path("id").asText();var i=createItem(c,"Curry");String id=i.path("id").asText();
        assertThat(i.path("sellingPrice").decimalValue()).isEqualByComparingTo("80.00");assertThat(i.path("effectiveAvailable").asBoolean()).isFalse();
        call("GET",path("/menu/items?foodType=VEG&available=true&search=Curry"),null,owner).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        call("DELETE",path("/menu/categories/"+c+"?restaurantVersion="+version),null,owner).andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("MENU_CATEGORY_NOT_EMPTY"));
        capture(call("PATCH",path("/menu/items/"+id+"/availability"),body(Map.of("available",false)),owner).andExpect(status().isOk()));
        capture(call("DELETE",path("/menu/items/"+id+"?restaurantVersion="+version),null,owner).andExpect(status().isOk()));
        call("GET",path("/menu/items/"+id),null,owner).andExpect(status().isNotFound());
        capture(call("DELETE",path("/menu/categories/"+c+"?restaurantVersion="+version),null,owner).andExpect(status().isOk()));
        assertThat(jdbc.queryForObject("select count(*) from menu_variants where deleted_at is not null",Integer.class)).isEqualTo(1);
    }
    @Test void menuCaseInsensitiveNamesAndPricesValidated() throws Exception {
        String c=category("Main").path("id").asText();call("POST",path("/menu/categories"),body(Map.of("name","main","displayOrder",0,"active",true)),owner).andExpect(status().isConflict());
        createItem(c,"Curry");call("POST",path("/menu/items"),item(c,"curry"),owner).andExpect(status().isConflict());
        var q=item(c,"Rice");q.put("discountPrice",101);call("POST",path("/menu/items"),q,owner).andExpect(status().isBadRequest());
        q=item(c,"Rice");q.put("basePrice",-1);call("POST",path("/menu/items"),q,owner).andExpect(status().isBadRequest());
        q=item(c,"Rice");((ObjectNode)q.path("addons").get(0)).put("price",1.001);call("POST",path("/menu/items"),q,owner).andExpect(status().isBadRequest());
    }
    @Test void categoryAndItemIsolationAcrossRestaurantsAndOwners() throws Exception {
        var first=restaurant;String c=category("Main").path("id").asText();String i=createItem(c,"Curry").path("id").asText();
        call("GET",path("/menu/items/"+i),null,other).andExpect(status().isNotFound());
        call("PATCH",path("/menu/items/"+i+"/availability"),body(Map.of("available",false)),other).andExpect(status().isNotFound());
        var r=data(call("POST","/owner/restaurants",Map.of("restaurantName","Second","restaurantType","RESTAURANT","email","second@example.test","phone","+919876543211"),owner).andExpect(status().isCreated()));restaurant=r.path("id").asText();version=r.path("version").asLong();
        call("GET",path("/menu/categories/"+c),null,owner).andExpect(status().isNotFound());
        call("POST",path("/menu/items"),item(c,"Cross tenant"),owner).andExpect(status().isNotFound());
        call("GET",path("/menu/items/"+i),null,owner).andExpect(status().isNotFound());assertThat(first).isNotEqualTo(restaurant);
    }
    @Test void optionIdsStableAndOmittedOptionsArchived() throws Exception {
        String c=category("Main").path("id").asText();var i=createItem(c,"Curry");var q=item(c,"Curry");String variant=i.path("variants").get(0).path("id").asText();
        ((ObjectNode)q.path("variants").get(0)).put("id",variant);((ObjectNode)q.path("variants").get(0)).put("name","Family");q.set("addons",json.createArrayNode());
        var updated=capture(call("PUT",path("/menu/items/"+i.path("id").asText()),q,owner).andExpect(status().isOk()));
        assertThat(updated.path("variants").get(0).path("id").asText()).isEqualTo(variant);assertThat(updated.path("addons").size()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from menu_addons where deleted_at is not null",Integer.class)).isEqualTo(1);
    }
    @Test void optionNameSwapsRetainIdentity() throws Exception {
        String c=category("Main").path("id").asText();var q=item(c,"Curry");((ArrayNode)q.path("variants")).add(json.valueToTree(Map.of("name","Small","price",90,"active",true)));
        var i=capture(call("POST",path("/menu/items"),q,owner).andExpect(status().isCreated()));var options=json.createArrayNode();
        for(var v:i.path("variants"))options.add(json.valueToTree(Map.of("id",v.path("id").asText(),"name",v.path("name").asText().equals("Large")?"Small":"Large","price",v.path("price").decimalValue(),"active",true)));
        q=item(c,"Curry");q.set("variants",options);var updated=capture(call("PUT",path("/menu/items/"+i.path("id").asText()),q,owner).andExpect(status().isOk()));
        for(var v:updated.path("variants")){var expected=options.findValuesAsText("id");assertThat(expected).contains(v.path("id").asText());for(var o:options)if(o.path("id").equals(v.path("id")))assertThat(v.path("name").asText()).isEqualTo(o.path("name").asText());}
        assertThat(jdbc.queryForObject("select count(*) from menu_variants",Integer.class)).isEqualTo(2);
    }
    @Test void crossItemAndDuplicateOptionIdsRejectedAtomically() throws Exception {
        String c=category("Main").path("id").asText();var a=createItem(c,"Curry");var b=createItem(c,"Rice");var q=item(c,"Rice");((ObjectNode)q.path("variants").get(0)).put("id",a.path("variants").get(0).path("id").asText());
        call("PUT",path("/menu/items/"+b.path("id").asText()),q,owner).andExpect(status().isNotFound());
        q=item(c,"Rice");((ArrayNode)q.path("variants")).add(q.path("variants").get(0).deepCopy());call("PUT",path("/menu/items/"+b.path("id").asText()),q,owner).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from menu_variants where deleted_at is null",Integer.class)).isEqualTo(2);
    }
    @Test void approvedInventoryEditableButInactiveCategoryNotSellable() throws Exception {
        approved();createTable("Live");String c=category("Main").path("id").asText();var i=createItem(c,"Curry");assertThat(i.path("effectiveAvailable").asBoolean()).isTrue();
        capture(call("PUT",path("/menu/categories/"+c),body(Map.of("name","Main","displayOrder",0,"active",false)),owner).andExpect(status().isOk()));
        call("GET",path("/menu/items/"+i.path("id").asText()),null,owner).andExpect(status().isOk()).andExpect(jsonPath("$.data.resource.effectiveAvailable").value(false));
    }
    @Test void publicAvailabilityGatedAndDoesNotExposePrivateFields() throws Exception {
        createTable("A");call("GET",availability("12:00"),null,null).andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
        approved();var response=call("GET",availability("12:00"),null,null).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.available").value(true));
        assertThat(data(response).toString()).doesNotContain("ownerId","accountNumber","settlementProfile","email","referenceId");
        call("GET",availability("12:00"),null,null).andExpect(status().isOk());assertThat(jdbc.queryForObject("select count(*) from table_allocations",Integer.class)).isZero();
        jdbc.update("update restaurant_service_activations set enabled=false where restaurant_id=?",UUID.fromString(restaurant));call("GET",availability("12:00"),null,null).andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
    }
    @Test void malformedAvailabilityAndPaginationReturn400() throws Exception {
        call("GET",availability("bad"),null,null).andExpect(status().isBadRequest());call("GET",availability(null)+"&offset=bad",null,null).andExpect(status().isBadRequest());
        call("GET","/restaurants/"+restaurant+"/availability?date="+date+"&guests=0",null,null).andExpect(status().isBadRequest());
        call("GET",path("/tables?size=101"),null,owner).andExpect(status().isBadRequest());call("GET",path("/menu/items?page=-1"),null,owner).andExpect(status().isBadRequest());
    }
    @Test void explicitClosuresAndTableStatusRemoveAvailability() throws Exception {
        approved();var t=createTable("A");capture(call("PATCH",path("/tables/"+t.path("id").asText()+"/status"),body(Map.of("status","MAINTENANCE","reason","Repair")),owner).andExpect(status().isOk()));
        call("GET",availability("12:00"),null,null).andExpect(status().isOk()).andExpect(jsonPath("$.data.reason").value("NO_TABLE_CAPACITY"));
        capture(call("PATCH",path("/tables/"+t.path("id").asText()+"/status"),body(Map.of("status","AVAILABLE","reason","Ready")),owner).andExpect(status().isOk()));
        jdbc.update("insert into restaurant_special_schedules(id,restaurant_id,date,closed,closes_next_day,reason) values (?,?,?,true,false,'Holiday')",UUID.randomUUID(),UUID.fromString(restaurant),java.sql.Date.valueOf(date));
        call("GET",availability("12:00"),null,null).andExpect(status().isOk()).andExpect(jsonPath("$.data.reason").value("CLOSED"));
    }
    @Test void claimRequiresOuterTransactionAndIsIdempotent() throws Exception {
        approved();UUID table=UUID.fromString(createTable("A").path("id").asText());UUID ref=UUID.randomUUID();
        assertThatThrownBy(()->allocations.claimReservation(actor,UUID.fromString(restaurant),table,ref,starts(),2)).isInstanceOf(IllegalTransactionStateException.class);
        var a=claim(table,ref);assertThat(claim(table,ref).id()).isEqualTo(a.id());assertThat(Duration.between(a.startsAt(),a.occupiedUntil()).toMinutes()).isEqualTo(75);
        assertThatThrownBy(()->new TransactionTemplate(transactions).execute(s->allocations.claimReservation(actor,UUID.fromString(restaurant),table,ref,starts(),3))).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("select count(*) from table_allocations",Integer.class)).isEqualTo(1);
    }
    @Test void claimProtectsAvailabilityAndCannotBeClearedByTableStatus() throws Exception {
        approved();String table=createTable("A").path("id").asText();claim(UUID.fromString(table),UUID.randomUUID());
        call("GET",availability("12:00"),null,null).andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
        call("PUT",path("/tables/"+table),table("A"),owner).andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("TABLE_IN_USE"));
        call("DELETE",path("/tables/"+table+"?restaurantVersion="+version),null,owner).andExpect(status().isConflict());
        capture(call("PATCH",path("/tables/"+table+"/status"),body(Map.of("status","AVAILABLE","reason","No cancellation")),owner).andExpect(status().isOk()));
        call("GET",availability("12:00"),null,null).andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
    }
    @Test void releaseScopedToReferenceAndMakesIntervalReusable() throws Exception {
        approved();UUID table=UUID.fromString(createTable("A").path("id").asText());UUID ref=UUID.randomUUID();var a=claim(table,ref);
        assertThatThrownBy(()->new TransactionTemplate(transactions).executeWithoutResult(s->allocations.release(actor,UUID.fromString(restaurant),a.id(),UUID.randomUUID()))).isInstanceOf(ApiException.class);
        new TransactionTemplate(transactions).executeWithoutResult(s->allocations.release(actor,UUID.fromString(restaurant),a.id(),ref));
        new TransactionTemplate(transactions).executeWithoutResult(s->allocations.release(actor,UUID.fromString(restaurant),a.id(),ref));
        assertThat(claim(table,UUID.randomUUID()).id()).isNotEqualTo(a.id());
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='TABLE_ALLOCATION_RELEASED'",Integer.class)).isEqualTo(1);
    }
    @Test void databaseRejectsOverlapButAcceptsHalfOpenAdjacency() throws Exception {
        UUID table=UUID.fromString(createTable("A").path("id").asText());raw(table,starts(),starts().plusHours(1));
        assertThatThrownBy(()->raw(table,starts().plusMinutes(30),starts().plusHours(2))).isInstanceOf(DataIntegrityViolationException.class);
        raw(table,starts().plusHours(1),starts().plusHours(2));assertThat(jdbc.queryForObject("select count(*) from table_allocations",Integer.class)).isEqualTo(2);
    }
    @Test void concurrentClaimsHaveOneWinner() throws Exception {
        approved();UUID table=UUID.fromString(createTable("A").path("id").asText());var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{Callable<Boolean> task=()->{start.await();try{claim(table,UUID.randomUUID());return true;}catch(ApiException ex){return false;}};var a=pool.submit(task);var b=pool.submit(task);start.countDown();assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);}finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("select count(*) from table_allocations where active",Integer.class)).isEqualTo(1);
    }
    @Test void exclusionConstraintAlsoProtectsConcurrentDirectWriters() throws Exception {
        UUID table=UUID.fromString(createTable("A").path("id").asText());var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{Callable<Boolean> task=()->{start.await();try{raw(table,starts(),starts().plusHours(1));return true;}catch(DataIntegrityViolationException ex){return false;}};var a=pool.submit(task);var b=pool.submit(task);start.countDown();assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);}finally{pool.shutdownNow();}
    }
    @Test void failedOuterTransactionRollsBackAllocationAndAudit() throws Exception {
        approved();UUID table=UUID.fromString(createTable("A").path("id").asText());
        assertThatThrownBy(()->new TransactionTemplate(transactions).executeWithoutResult(s->{allocations.claimReservation(actor,UUID.fromString(restaurant),table,UUID.randomUUID(),starts(),2);throw new IllegalStateException("Simulated booking persistence failure");})).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select count(*) from table_allocations",Integer.class)).isZero();assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='TABLE_ALLOCATION_CREATED'",Integer.class)).isZero();
    }
    @Test void availabilityRateLimitCannotBeEvadedWithDifferentRestaurantIds() throws Exception {
        for(int i=0;i<60;i++)call("GET","/restaurants/"+UUID.randomUUID()+"/availability?date="+date+"&guests=2",null,null).andExpect(status().isOk());
        call("GET",availability("12:00"),null,null).andExpect(status().isTooManyRequests());
    }
    @Test void databaseRejectsCrossRestaurantTableReference() throws Exception {
        UUID table=UUID.fromString(createTable("A").path("id").asText());
        var r=data(call("POST","/owner/restaurants",Map.of("restaurantName","Second","restaurantType","RESTAURANT","email","second@example.test","phone","+919876543211"),owner).andExpect(status().isCreated()));restaurant=r.path("id").asText();
        assertThatThrownBy(()->raw(table,starts(),starts().plusHours(1))).isInstanceOf(DataIntegrityViolationException.class);
    }

}
