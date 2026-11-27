package com.bookeat.user.service.impl;
import com.bookeat.customer.repository.CustomerRepository;
import com.bookeat.owner.repository.RestaurantOwnerRepository;
import com.bookeat.restaurant.repository.RestaurantRepository;
import com.bookeat.exception.*;
import com.bookeat.user.entity.UserStatus;
import com.bookeat.user.mapper.UserMapper;
import com.bookeat.user.response.ProfileResponse;
import com.bookeat.user.service.ProfileService;
import com.bookeat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ProfileServiceImpl implements ProfileService {
    private final CustomerRepository customers;
    private final RestaurantOwnerRepository owners;
    private final UserMapper mapper;
    private final UserRepository users;
    private final RestaurantRepository restaurants;
    @Override public ProfileResponse customer(UUID userId) {
        var customer = customers.findByUserId(userId).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        return new ProfileResponse(customer.getId(), mapper.toResponse(customer.getUser()));
    }
    @Override
    @Transactional
    public ProfileResponse updateCustomer(UUID userId, com.bookeat.customer.dto.CustomerProfileUpdateRequest req) {
        var customer = customers.findByUserId(userId).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        var user = customer.getUser();
        var normalizedEmail = req.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalizedEmail.equalsIgnoreCase(user.getEmail()) && users.existsByEmail(normalizedEmail)) {
            throw new ApiException(ErrorCode.USER_ALREADY_EXISTS);
        }
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setPhoneNumber(req.phoneNumber().trim());
        if (!normalizedEmail.equalsIgnoreCase(user.getEmail())) {
            user.setEmail(normalizedEmail);
            user.setEmailVerified(false);
        }
        users.saveAndFlush(user);
        return new ProfileResponse(customer.getId(), mapper.toResponse(user));
    }

    @Override
    @Transactional
    public void deleteCustomer(UUID userId) {
        var customerOpt = customers.findByUserId(userId);
        customerOpt.ifPresent(c -> {
            customers.delete(c);
            customers.flush();
        });
        var userOpt = users.findById(userId);
        if (userOpt.isPresent()) {
            var user = userOpt.get();
            user.setStatus(UserStatus.INACTIVE);
            user.setEmail("deleted_" + System.currentTimeMillis() + "_" + user.getEmail());
            user.setPhoneNumber(null);
            user.setTokenVersion(user.getTokenVersion() + 1);
            users.saveAndFlush(user);
        }
    }

    @Override
    @Transactional
    public ProfileResponse owner(UUID userId) {
        var owner = owners.findByUserId(userId).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        var user = owner.getUser();
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank() || user.getPhoneNumber().contains("1234567890")) {
            var venues = restaurants.findByOwnerUserId(userId, org.springframework.data.domain.Pageable.unpaged());
            if (!venues.isEmpty() && venues.getContent().get(0).getPhone() != null && !venues.getContent().get(0).getPhone().isBlank()) {
                user.setPhoneNumber(venues.getContent().get(0).getPhone().trim());
                users.saveAndFlush(user);
            }
        }
        return new ProfileResponse(owner.getId(), mapper.toResponse(user));
    }
    @Override
    @Transactional
    public ProfileResponse updateOwner(UUID userId, com.bookeat.user.request.UpdateProfileRequest req) {
        var owner = owners.findByUserId(userId).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        var user = owner.getUser();
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setPhoneNumber(req.phoneNumber());
        users.saveAndFlush(user);
        if (req.phoneNumber() != null && !req.phoneNumber().isBlank()) {
            var venues = restaurants.findByOwnerUserId(userId, org.springframework.data.domain.Pageable.unpaged());
            for (var r : venues) {
                r.setPhone(req.phoneNumber().trim());
                restaurants.save(r);
            }
        }
        return new ProfileResponse(owner.getId(), mapper.toResponse(user));
    }
}
