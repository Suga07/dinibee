package com.bookeat.user.service;
import com.bookeat.user.response.ProfileResponse;
import java.util.UUID;
public interface ProfileService {
    ProfileResponse customer(UUID userId);
    ProfileResponse updateCustomer(UUID userId, com.bookeat.customer.dto.CustomerProfileUpdateRequest req);
    void deleteCustomer(UUID userId);
    ProfileResponse owner(UUID userId);
    ProfileResponse updateOwner(UUID userId, com.bookeat.user.request.UpdateProfileRequest req);
}
