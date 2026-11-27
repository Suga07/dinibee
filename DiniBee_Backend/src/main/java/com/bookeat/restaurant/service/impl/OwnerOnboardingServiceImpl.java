package com.bookeat.restaurant.service.impl;
import com.bookeat.restaurant.dto.RestaurantRequests.*;
import com.bookeat.restaurant.dto.RestaurantResponses.*;
import com.bookeat.restaurant.entity.*;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.repository.*;
import com.bookeat.restaurant.service.*;
import com.bookeat.restaurant.mapper.RestaurantMapper;
import com.bookeat.restaurant.security.SettlementEncryptionService;
import com.bookeat.owner.repository.RestaurantOwnerRepository;
import com.bookeat.table.entity.RestaurantTable;
import com.bookeat.table.enums.TableShape;
import com.bookeat.table.enums.TableStatus;
import com.bookeat.table.repository.RestaurantTableRepository;
import com.bookeat.user.mapper.UserMapper;
import com.bookeat.common.response.PageResponse;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
@PreAuthorize("hasRole('OWNER') and hasAuthority('RESTAURANT_OWN_MANAGE')")
public class OwnerOnboardingServiceImpl implements OwnerOnboardingService {
    private final RestaurantRepository restaurants;
    private final RestaurantOwnerRepository owners;
    private final RestaurantAccessService access;
    private final RestaurantMapper mapper;
    private final OnboardingProgressService progress;
    private final RestaurantChangeService changes;
    private final SettlementEncryptionService encryption;
    private final UserMapper users;
    private final Clock clock;
    private final RestaurantServiceActivationRepository restaurantServiceActivation;
    private final RestaurantOperatingHourRepository restaurantOperatingHour;
    private final RestaurantSpecialScheduleRepository restaurantSpecialSchedule;
    private final RestaurantDocumentRepository restaurantDocument;
    private final RestaurantReservationSettingsRepository restaurantReservationSettings;
    private final RestaurantTakeawaySettingsRepository restaurantTakeawaySettings;
    private final DiniTicketSettingsRepository diniTicketSettings;
    private final RestaurantPolicyRepository restaurantPolicy;
    private final RestaurantSettlementProfileRepository restaurantSettlementProfile;
    private final RestaurantTableSetupRepository restaurantTableSetup;
    private final RestaurantTableRepository liveTables;
    private final RestaurantMenuSetupRepository restaurantMenuSetup;
    private final RestaurantTeamAccessRepository restaurantTeamAccess;
    private final RestaurantTeamDraftRepository restaurantTeamDraft;
    private final com.bookeat.user.repository.UserRepository userRepository;
    @Override @PreAuthorize("hasRole('OWNER') and hasAuthority('RESTAURANT_CREATE')")
    public Detail create(UUID actor,CreateRestaurant q) {
        var owner=owners.findByUserId(actor).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        var r=new Restaurant();r.setOwner(owner);r.setRestaurantName(q.restaurantName().trim());r.setRestaurantType(q.restaurantType());
        r.setEmail(q.email().trim().toLowerCase(Locale.ROOT));r.setPhone(q.phone());r.setApprovalStatus(ApprovalStatus.DRAFT);r.setStatus(RestaurantStatus.INACTIVE);
        restaurants.save(r);
        // Removed: for(ServiceType type:ServiceType.values()) { ... }
        return finish(r,actor,"RESTAURANT_CREATED");
    }
    @Override @Transactional(readOnly=true)
    public PageResponse<Summary> list(UUID actor,int page,int size) {
        return PageResponse.from(restaurants.findByOwnerUserId(actor,PageRequest.of(page,size,Sort.by("createdAt").descending().and(Sort.by("id")))).map(mapper::summary));
    }
    @Override @Transactional(readOnly=true)
    public Detail get(UUID actor,UUID id){return mapper.detail(access.getOwnedRestaurant(actor,id),false);}
    @Override @Transactional(readOnly=true)
    public Progress progress(UUID actor,UUID id){return progress.calculate(access.getOwnedRestaurant(actor,id));}
    @Override @Transactional(readOnly=true)
    public DashboardContext dashboard(UUID actor,UUID id) {
        var owner=owners.findByUserId(actor).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));var u=owner.getUser();
        Restaurant r=id==null?restaurants.findFirstByOwnerUserIdOrderByCreatedAtAscIdAsc(actor).orElse(null):access.getOwnedRestaurant(actor,id);
        var p=r==null?null:progress.calculate(r);
        return new DashboardContext(new Owner(owner.getId(),u.getId(),u.getFirstName(),u.getLastName(),u.getEmail()),r==null?null:mapper.summary(r),
            r==null?null:r.getApprovalStatus(),r==null?null:r.getStatus(),r==null?Set.of():mapper.enabledServices(r),users.permissions(u),p!=null && p.complete(),p);
    }
    private Restaurant editable(UUID actor,UUID id,long version) {
        var r=access.lockOwnedRestaurant(actor,id,version);
        if(!WorkflowRules.settingsEditable(r.getApprovalStatus())) throw new ApiException(ErrorCode.RESTAURANT_STATE_INVALID);
        return r;
    }
    private Detail finish(Restaurant r,UUID actor,String action){changes.finish(r,actor,action);return mapper.detail(r,false);}
    private static void require(boolean condition){if(!condition) throw new ApiException(ErrorCode.VALIDATION_FAILED);}

    @Override public Detail updateDetails(UUID actor,UUID id,Details q) {
        var r=editable(actor,id,q.version());
        r.setRestaurantName(q.restaurantName());
        r.setDescription(q.description());
        r.setRestaurantType(q.restaurantType());
        r.setEmail(q.email().trim().toLowerCase(Locale.ROOT));
        r.setPhone(q.phone());
        r.setAlternatePhone(q.alternatePhone());
        if (q.phone() != null && !q.phone().isBlank()) {
            if (r.getOwner() != null && r.getOwner().getUser() != null) {
                var u = r.getOwner().getUser();
                u.setPhoneNumber(q.phone().trim());
                userRepository.saveAndFlush(u);
            }
        }
        // Null from a details-only client must not erase an uploaded asset.
        if(q.logoUrl()!=null) r.setLogoUrl(q.logoUrl());
        if(q.coverImageUrl()!=null) r.setCoverImageUrl(q.coverImageUrl());
        return finish(r,actor,"ONBOARDING_DETAILS_UPDATED");
        }

    @Override public Detail updateLocation(UUID actor,UUID id,Location q) {
        var r=editable(actor,id,q.version());
        try {ZoneId.of(q.timezone());} catch(DateTimeException ex){throw new ApiException(ErrorCode.VALIDATION_FAILED);}
        r.setAddressLine1(q.addressLine1());
        r.setAddressLine2(q.addressLine2());
        r.setCity(q.city());
        r.setState(q.state());
        r.setPostalCode(q.postalCode());
        r.setCountry(q.country());
        r.setLatitude(q.latitude());
        r.setLongitude(q.longitude());
        r.setTimezone(q.timezone());
        return finish(r,actor,"ONBOARDING_LOCATION_UPDATED");
        }

    @Override public Detail updateBusinessInfo(UUID actor,UUID id,BusinessInfo q) {
        var r=editable(actor,id,q.version());
        boolean changed=!Objects.equals(r.getBusinessRegistrationNumber(),q.businessRegistrationNumber()) || !Objects.equals(r.getFssaiNumber(),q.fssaiNumber()) || !Objects.equals(r.getGstNumber(),q.gstNumber());
        if(changed) {
            restaurantDocument.findByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(id).forEach(d->{
                if(d.getVerificationStatus()==VerificationStatus.VERIFIED) {d.setVerificationStatus(VerificationStatus.PENDING);d.setVerifiedBy(null);d.setVerifiedAt(null);}
            });
            restaurantSettlementProfile.findByRestaurantId(id).ifPresent(p->{p.setVerified(false);p.setVerifiedBy(null);p.setVerifiedAt(null);});
        }
        r.setBusinessRegistrationNumber(q.businessRegistrationNumber());
        r.setGstNumber(q.gstNumber());
        r.setFssaiNumber(q.fssaiNumber());
        return finish(r,actor,"ONBOARDING_BUSINESSINFO_UPDATED");
        }

    @Override public Detail updateReservationSettings(UUID actor,UUID id,ReservationSettings q) {
        var r=editable(actor,id,q.version());
        require(q.minimumGuests()<=q.maximumGuestsPerBooking() && q.slotDurationMinutes()<=q.reservationDurationMinutes());
        require(q.minimumAdvanceMinutes()<=q.advanceBookingDays()*1440 && q.cancellationBeforeMinutes()<=q.advanceBookingDays()*1440);
        var e=restaurantReservationSettings.findByRestaurantId(id).orElseGet(RestaurantReservationSettings::new);
        e.setRestaurant(r);
        e.setReservationEnabled(q.reservationEnabled());
        e.setMinimumGuests(q.minimumGuests());
        e.setMaximumGuestsPerBooking(q.maximumGuestsPerBooking());
        e.setSlotDurationMinutes(q.slotDurationMinutes());
        e.setReservationDurationMinutes(q.reservationDurationMinutes());
        e.setAdvanceBookingDays(q.advanceBookingDays());
        e.setMinimumAdvanceMinutes(q.minimumAdvanceMinutes());
        e.setGracePeriodMinutes(q.gracePeriodMinutes());
        e.setCancellationAllowed(q.cancellationAllowed());
        e.setCancellationBeforeMinutes(q.cancellationBeforeMinutes());
        e.setAutoConfirm(q.autoConfirm());
        e.setSpecialRequestAllowed(q.specialRequestAllowed());
        e.setPreOrderAllowed(q.preOrderAllowed());
        e.setGpsCheckInEnabled(q.gpsCheckInEnabled());
        e.setQrCheckInEnabled(q.qrCheckInEnabled());
        e.setGpsCheckInRadiusMeters(q.gpsCheckInRadiusMeters());
        restaurantReservationSettings.save(e);
        return finish(r,actor,"ONBOARDING_RESERVATIONSETTINGS_UPDATED");
        }

    @Override public Detail updateTakeawaySettings(UUID actor,UUID id,TakeawaySettings q) {
        var r=editable(actor,id,q.version());
        require(q.maximumAdvanceOrderMinutes()>=q.averagePreparationMinutes());
        require(!q.enabled() || ((q.regularTakeawayEnabled() || q.onTheWayEnabled()) && (q.acceptCash() || q.acceptOnlinePayment())));
        var e=restaurantTakeawaySettings.findByRestaurantId(id).orElseGet(RestaurantTakeawaySettings::new);
        e.setRestaurant(r);
        e.setEnabled(q.enabled());
        e.setRegularTakeawayEnabled(q.regularTakeawayEnabled());
        e.setOnTheWayEnabled(q.onTheWayEnabled());
        e.setMinimumOrderAmount(q.minimumOrderAmount());
        e.setAveragePreparationMinutes(q.averagePreparationMinutes());
        e.setMaximumAdvanceOrderMinutes(q.maximumAdvanceOrderMinutes());
        e.setAcceptCash(q.acceptCash());
        e.setAcceptOnlinePayment(q.acceptOnlinePayment());
        restaurantTakeawaySettings.save(e);
        return finish(r,actor,"ONBOARDING_TAKEAWAYSETTINGS_UPDATED");
        }

    @Override public Detail updateDiniSettings(UUID actor,UUID id,DiniSettings q) {
        var r=editable(actor,id,q.version());
        var e=diniTicketSettings.findByRestaurantId(id).orElseGet(DiniTicketSettings::new);
        e.setRestaurant(r);
        e.setEnabled(q.enabled());
        e.setMaximumQueueSize(q.maximumQueueSize());
        e.setDefaultEstimatedMinutesPerParty(q.defaultEstimatedMinutesPerParty());
        e.setAllowPreOrder(q.allowPreOrder());
        e.setAllowRemoteJoin(q.allowRemoteJoin());
        e.setTicketExpiryMinutes(q.ticketExpiryMinutes());
        e.setAutoExpireTickets(q.autoExpireTickets());
        diniTicketSettings.save(e);
        return finish(r,actor,"ONBOARDING_DINISETTINGS_UPDATED");
        }

    @Override public Detail updatePolicies(UUID actor,UUID id,Policies q) {
        var r=editable(actor,id,q.version());
        var e=restaurantPolicy.findByRestaurantId(id).orElseGet(RestaurantPolicy::new);
        e.setRestaurant(r);
        e.setCancellationPolicy(q.cancellationPolicy());
        e.setRefundPolicy(q.refundPolicy());
        e.setChildPolicy(q.childPolicy());
        e.setPetPolicy(q.petPolicy());
        e.setDressCode(q.dressCode());
        e.setAdditionalPolicy(q.additionalPolicy());
        restaurantPolicy.save(e);
        return finish(r,actor,"ONBOARDING_POLICIES_UPDATED");
        }

    @Override public Detail updateTableSetup(UUID actor,UUID id,TableSetup q) {
        var r=editable(actor,id,q.version());
        require(q.plannedSeatCount()>=q.plannedTableCount() && q.plannedSeatCount()<=q.plannedTableCount()*30);
        var e=restaurantTableSetup.findByRestaurantId(id).orElseGet(RestaurantTableSetup::new);
        e.setRestaurant(r);
        e.setPlannedTableCount(q.plannedTableCount());
        e.setPlannedSeatCount(q.plannedSeatCount());
        e.setSectionDescription(q.sectionDescription());
        restaurantTableSetup.save(e);
        // Table Setup is part of onboarding, so it must create real restaurant_tables rows.
        // Previously only the planned counts were saved, which made customer availability
        // empty for newly onboarded restaurants even though the owner had entered tables.
        materializeOnboardingTables(r, q);
        return finish(r,actor,"ONBOARDING_TABLESETUP_UPDATED");
        }

    private void materializeOnboardingTables(Restaurant r, TableSetup q) {
        long existing = liveTables.countByRestaurantIdAndDeletedAtIsNull(r.getId());
        if (existing >= q.plannedTableCount()) return;
        int count = q.plannedTableCount();
        int seats = q.plannedSeatCount();
        int base = seats / count;
        int extra = seats % count;
        String section = q.sectionDescription();
        if (section != null) { section = section.trim(); if (section.length() > 100) section = section.substring(0, 100); }
        for (int i=(int)existing + 1; i<=count; i++) {
            int capacity = base + (i <= extra ? 1 : 0);
            RestaurantTable t = new RestaurantTable();
            t.setRestaurant(r); t.setTableNumber("T" + i); t.setTableName("Table " + i);
            t.setCapacity(capacity); t.setMinimumGuests(1); t.setMaximumGuests(capacity);
            t.setFloor(0); t.setSection(section); t.setShape(TableShape.SQUARE); t.setStatus(TableStatus.AVAILABLE);
            int index=i-1; t.setXPosition(BigDecimal.valueOf((index % 4) * 120L));
            t.setYPosition(BigDecimal.valueOf((index / 4) * 120L));
            t.setWidth(BigDecimal.valueOf(100)); t.setHeight(BigDecimal.valueOf(100));
            t.setRotation(BigDecimal.ZERO); t.setActive(true); liveTables.save(t);
        }
    }

    @Override public Detail updateMenuSetup(UUID actor,UUID id,MenuSetup q) {
        var r=editable(actor,id,q.version());
        require(q.mode()!=MenuSetupMode.EXTERNAL_MENU || (q.externalMenuUrl()!=null && !q.externalMenuUrl().isBlank()));
        var e=restaurantMenuSetup.findByRestaurantId(id).orElseGet(RestaurantMenuSetup::new);
        e.setRestaurant(r);
        e.setMode(q.mode());
        e.setExternalMenuUrl(q.externalMenuUrl());
        e.setNotes(q.notes());
        e.setAcknowledged(q.acknowledged());
        restaurantMenuSetup.save(e);
        return finish(r,actor,"ONBOARDING_MENUSETUP_UPDATED");
        }

    @Override public Detail updateSettlementProfile(UUID actor,UUID id,SettlementProfile q) {
        var r=editable(actor,id,q.version());
        var e=restaurantSettlementProfile.findByRestaurantId(id).orElseGet(RestaurantSettlementProfile::new);
        e.setRestaurant(r);
        e.setAccountHolderName(q.accountHolderName());
        e.setBankName(q.bankName());
        e.setIfscCode(q.ifscCode());
        e.setUpiId(q.upiId());
        e.setTaxName(q.taxName());
        require(Objects.equals(r.getGstNumber(),q.gstNumber()));
        e.setGstNumber(q.gstNumber());
        e.setEncryptedAccountNumber(encryption.encrypt(id,q.accountNumber()));e.setAccountNumberLast4(q.accountNumber().substring(q.accountNumber().length()-4));
        e.setVerified(false);e.setVerifiedBy(null);e.setVerifiedAt(null);e.setRejectionReason(null);
        restaurantSettlementProfile.save(e);
        return finish(r,actor,"ONBOARDING_SETTLEMENTPROFILE_UPDATED");
        }

    @Override public Detail updateHours(UUID actor,UUID id,Hours q) {
        var r=editable(actor,id,q.version());
        require(q.hours().stream().map(Hour::dayOfWeek).distinct().count()==7);
        q.hours().forEach(h->require(WorkflowRules.validHours(h.closed(),h.openingTime(),h.closingTime(),h.closesNextDay())));
        restaurantOperatingHour.deleteByRestaurantId(id);restaurantOperatingHour.flush();
        q.hours().forEach(h->{var e=new RestaurantOperatingHour();e.setRestaurant(r);e.setDayOfWeek(h.dayOfWeek());e.setOpeningTime(h.openingTime());e.setClosingTime(h.closingTime());e.setClosed(h.closed());e.setClosesNextDay(h.closesNextDay());restaurantOperatingHour.save(e);});
        return finish(r,actor,"ONBOARDING_HOURS_UPDATED");
    }
    @Override public Detail updateSpecialSchedules(UUID actor,UUID id,SpecialSchedules q) {
        var r=editable(actor,id,q.version());
        require(q.schedules().stream().map(SpecialSchedule::date).distinct().count()==q.schedules().size());
        q.schedules().forEach(h->require(WorkflowRules.validHours(h.closed(),h.openingTime(),h.closingTime(),h.closesNextDay())));
        restaurantSpecialSchedule.deleteByRestaurantId(id);restaurantSpecialSchedule.flush();
        q.schedules().forEach(h->{var e=new RestaurantSpecialSchedule();e.setRestaurant(r);e.setDate(h.date());e.setOpeningTime(h.openingTime());e.setClosingTime(h.closingTime());e.setClosed(h.closed());e.setClosesNextDay(h.closesNextDay());e.setReason(h.reason());restaurantSpecialSchedule.save(e);});
        return finish(r,actor,"ONBOARDING_SPECIAL_SCHEDULES_UPDATED");
    }
    @Override public Detail selectServices(UUID actor,UUID id,ServiceSelection q) {
        var r=editable(actor,id,q.version());
        var existing = restaurantServiceActivation.findByRestaurantIdOrderByCreatedAtAscIdAsc(id);
        var existingTypes = existing.stream().map(RestaurantServiceActivation::getServiceType).toList();
        for (ServiceType type : q.services()) {
            if (!existingTypes.contains(type)) {
                var s = new RestaurantServiceActivation();
                s.setRestaurant(r);
                s.setServiceType(type);
                s.setRequested(true);
                s.setAuthorized(false);
                s.setEnabled(false);
                restaurantServiceActivation.save(s);
            }
        }
        for (RestaurantServiceActivation s : existing) {
            if (q.services().contains(s.getServiceType())) {
                s.setRequested(true);
            } else {
                s.setRequested(false);
                s.setAuthorized(false);
                if(s.isEnabled()) s.setDisabledAt(clock.instant());
                s.setEnabled(false);
                restaurantServiceActivation.delete(s); // Optionally delete if the prompt says "Do not create unconfigured..." we just delete unselected.
            }
        }
        restaurantServiceActivation.flush();
        return finish(r,actor,"ONBOARDING_SERVICES_SELECTED");
    }
    @Override public Detail updateTeamAccess(UUID actor,UUID id,TeamAccess q) {
        var r=editable(actor,id,q.version());
        require(q.mode()==TeamAccessMode.OWNER_ONLY?q.members().isEmpty():!q.members().isEmpty());
        require(q.members().stream().map(m->m.email().trim().toLowerCase(Locale.ROOT)).distinct().count()==q.members().size());
        var e=restaurantTeamAccess.findByRestaurantId(id).orElseGet(RestaurantTeamAccess::new);
        e.setRestaurant(r);e.setMode(q.mode());e.setAcknowledged(q.acknowledged());restaurantTeamAccess.save(e);
        restaurantTeamDraft.deleteByRestaurantId(id);restaurantTeamDraft.flush();
        q.members().forEach(m->{var d=new RestaurantTeamDraft();d.setRestaurant(r);d.setEmail(m.email().trim().toLowerCase(Locale.ROOT));d.setRole(m.role());restaurantTeamDraft.save(d);});
        return finish(r,actor,"ONBOARDING_TEAM_PLAN_UPDATED");
    }
    @Override public Detail updateForm(UUID actor,UUID id,Form q) {
        var r=editable(actor,id,q.version());
        r.setOnboardingForm(q.form());
        syncExactOnboardingTables(r, q.form());
        return finish(r,actor,"ONBOARDING_FORM_UPDATED");
    }

    /**
     * Reconciles the exact table definitions entered in onboarding with the live
     * restaurant_tables inventory. The old aggregate-only materialisation could
     * turn e.g. 2x2-seat + 2x4-seat + 1x10-seat into averaged 7/8-seat tables.
     * The complete onboarding form contains the real per-table capacities, so
     * use it as the source of truth before submission.
     */
    private void syncExactOnboardingTables(Restaurant restaurant, com.bookeat.restaurant.dto.OnboardingForm form) {
        if (form == null || form.tables() == null || form.tables().isEmpty()) return;
        var existing = liveTables.findByRestaurantIdAndDeletedAtIsNullOrderByCapacityAscTableNumberAsc(restaurant.getId());
        Map<String, RestaurantTable> byNumber = new LinkedHashMap<>();
        for (RestaurantTable table : existing) {
            if (table.getTableNumber() != null) byNumber.put(table.getTableNumber().trim().toLowerCase(Locale.ROOT), table);
        }

        Set<String> desiredNumbers = new HashSet<>();
        int index = 0;
        for (var planned : form.tables()) {
            String number = planned.tableNumber().trim();
            if (number.isEmpty()) continue;
            String key = number.toLowerCase(Locale.ROOT);
            desiredNumbers.add(key);
            RestaurantTable table = byNumber.get(key);
            if (table == null) {
                table = new RestaurantTable();
                table.setRestaurant(restaurant);
                table.setTableNumber(number);
                table.setTableName(number);
                table.setFloor(0);
                table.setShape(TableShape.RECTANGLE);
                table.setStatus(TableStatus.AVAILABLE);
                table.setXPosition(BigDecimal.valueOf((index % 5) * 140L));
                table.setYPosition(BigDecimal.valueOf((index / 5) * 110L));
                table.setWidth(BigDecimal.valueOf(120));
                table.setHeight(BigDecimal.valueOf(80));
                table.setRotation(BigDecimal.ZERO);
            }
            table.setCapacity(planned.capacity());
            table.setMinimumGuests(1);
            table.setMaximumGuests(planned.capacity());
            table.setSection(planned.location() == null || planned.location().isBlank() ? null : planned.location().trim());
            table.setActive(planned.available());
            liveTables.save(table);
            index++;
        }

        // Disable only leftover auto-generated onboarding rows. They are the
        // rows that caused category averaging; custom operational tables are kept.
        for (RestaurantTable table : existing) {
            String number = table.getTableNumber() == null ? "" : table.getTableNumber().trim();
            String key = number.toLowerCase(Locale.ROOT);
            String name = table.getTableName() == null ? "" : table.getTableName().trim();
            boolean generated = number.matches("(?i)^T\\d+$") && name.matches("(?i)^Table\\s+\\d+$");
            if (generated && !desiredNumbers.contains(key)) {
                table.setActive(false);
                liveTables.save(table);
            }
        }
    }
    @Override public Detail submit(UUID actor,UUID id,VersionRequest q) {
        var r=editable(actor,id,q.version());var p=progress.calculate(r);
        if(!p.complete()) throw new OnboardingIncompleteException(p.missingSteps());
        var from=r.getApprovalStatus();r.setApprovalStatus(ApprovalStatus.SUBMITTED);r.setStatus(RestaurantStatus.INACTIVE);
        r.setSubmittedAt(clock.instant());r.setRejectionReason(null);r.setChangeRequestReason(null);
        changes.history(r,actor,ReviewAction.SUBMIT,from,null,null);
        return finish(r,actor,"RESTAURANT_SUBMITTED");
    }
}
