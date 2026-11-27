package com.bookeat.user.mapper;
import com.bookeat.user.entity.*;
import com.bookeat.user.response.UserResponse;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class UserMapper {
    public Set<String> roles(User user) {
        return user.getRoles().stream().map(r -> r.getCode().name()).collect(Collectors.toCollection(TreeSet::new));
    }
    public Set<String> permissions(User user) {
        return user.getRoles().stream().flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode).collect(Collectors.toCollection(TreeSet::new));
    }
    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getPhoneNumber(), user.getStatus(), user.isEmailVerified(),
                roles(user), permissions(user), user.getCreatedAt());
    }
}
