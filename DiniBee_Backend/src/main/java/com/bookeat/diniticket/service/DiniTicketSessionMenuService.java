package com.bookeat.diniticket.service;

import com.bookeat.exception.ApiException;
import com.bookeat.exception.ErrorCode;
import com.bookeat.menu.entity.MenuCategory;
import com.bookeat.menu.entity.MenuItem;
import com.bookeat.menu.enums.FoodType;
import com.bookeat.menu.enums.SpiceLevel;
import com.bookeat.menu.repository.MenuCategoryRepository;
import com.bookeat.menu.repository.MenuItemRepository;
import com.bookeat.restaurant.service.RestaurantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DiniTicketSessionMenuService {
    public static final String SESSION_CATEGORY = "DiniTicket Session";

    private final RestaurantAccessService access;
    private final MenuCategoryRepository categories;
    private final MenuItemRepository items;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final DiniTicketSessionService sessions;

    public record SessionMenuItem(UUID id, long version, String name, BigDecimal price, String imageUrl,
                                  int preparationTimeMinutes, boolean available) {}

    public record SaveItem(String name, BigDecimal price, String imageUrl,
                           Integer preparationTimeMinutes, Boolean available) {}

    @Transactional(readOnly = true)
    public List<SessionMenuItem> listOwner(UUID actor, UUID restaurantId) {
        access.getOwnedRestaurant(actor, restaurantId);
        return activeItems(restaurantId).stream().map(this::view).toList();
    }

    public List<SessionMenuItem> replace(UUID actor, UUID restaurantId, List<SaveItem> requested) {
        var restaurant = access.getOwnedRestaurant(actor, restaurantId);
        // A new menu after the previous session was archived starts a fresh
        // DiniTicket session. Editing the same live menu must preserve item IDs;
        // otherwise customers holding the currently displayed menu would submit
        // stale menuItemIds and receive MENU_ITEM_UNAVAILABLE (409).
        var existing = activeItems(restaurantId);
        boolean startingNewSession = existing.isEmpty();
        var category = sessionCategory(restaurantId);
        if (category == null) {
            category = new MenuCategory();
            category.setRestaurant(restaurant);
            category.setName(SESSION_CATEGORY);
            category.setDescription("Session-only food for DiniTicket");
            category.setDisplayOrder(9999);
            category.setActive(true);
            category.setDeletedAt(null);
            categories.saveAndFlush(category);
        } else {
            category.setActive(true);
            category.setDeletedAt(null);
        }

        var requestedItems = requested == null ? List.<SaveItem>of() : requested;
        var byName = new java.util.LinkedHashMap<String, MenuItem>();
        for (var item : existing) byName.put(item.getName().trim().toLowerCase(Locale.ROOT), item);
        var used = new java.util.HashSet<UUID>();
        var seenNames = new java.util.HashSet<String>();
        int displayOrder = 1;

        for (var q : requestedItems) {
            if (q == null || q.name() == null || q.name().trim().isEmpty() || q.price() == null || q.price().signum() < 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED);
            }
            String normalized = q.name().trim().toLowerCase(Locale.ROOT);
            if (!seenNames.add(normalized)) throw new ApiException(ErrorCode.VALIDATION_FAILED);

            var item = byName.get(normalized);
            if (item == null) {
                item = new MenuItem();
                item.setRestaurant(restaurant);
                item.setCategory(category);
            }
            item.setName(q.name().trim());
            item.setDescription("DiniTicket session food");
            item.setBasePrice(q.price());
            item.setDiscountPrice(null);
            item.setFoodType(FoodType.VEG);
            item.setSpiceLevel(SpiceLevel.NONE);
            item.setImageUrl(q.imageUrl() == null || q.imageUrl().isBlank() ? null : q.imageUrl().trim());
            item.setAvailable(q.available() == null || q.available());
            item.setPreOrderAvailable(false);
            item.setTakeawayAvailable(false);
            item.setDiniTicketPreOrderAvailable(true);
            item.setPreparationTimeMinutes(Math.max(0, q.preparationTimeMinutes() == null ? 0 : q.preparationTimeMinutes()));
            item.setFeatured(false);
            item.setDisplayOrder(displayOrder++);
            item.setDeletedAt(null);
            items.save(item);
            items.flush();
            used.add(item.getId());
        }

        final Instant now = clock.instant();
        for (var old : existing) {
            if (!used.contains(old.getId())) {
                old.setAvailable(false);
                old.setDeletedAt(now);
            }
        }
        items.flush();

        if (startingNewSession && !requestedItems.isEmpty()) {
            Integer max = jdbc.queryForObject("select coalesce(maximum_queue_size,100) from diniticket_settings where restaurant_id=?", Integer.class, restaurantId);
            sessions.startNew(restaurantId, max == null ? 100 : max);
        }
        return activeItems(restaurantId).stream().map(this::view).toList();
    }

    public String uploadImage(UUID actor, UUID restaurantId, MultipartFile file, String baseUri) {
        access.getOwnedRestaurant(actor, restaurantId);
        if (file == null || file.isEmpty()) throw new ApiException(ErrorCode.VALIDATION_FAILED);
        String original = file.getOriginalFilename();
        String ext = ".jpg";
        if (original != null && original.contains(".")) ext = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        if (!List.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) throw new ApiException(ErrorCode.VALIDATION_FAILED);
        String filename = "diniticket-session-" + UUID.randomUUID() + ext;
        try {
            Path dir = Path.of(System.getProperty("dinibee.menu.media.root", System.getProperty("user.home") + "/.dinibee/menu-images")).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename).normalize(), file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return baseUri + "/api/v1/public/media/" + filename;
        } catch (java.io.IOException ex) {
            throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        }
    }

    /**
     * DiniTicket session food is intentionally temporary. Once the number of
     * tickets issued since this food session started reaches the configured
     * session limit, soft-delete the food so it disappears from both customer
     * and owner DiniTicket Menu. Historical food/order rows stay intact.
     */
    public void archiveIfLimitReached(UUID restaurantId, int maxTokens) {
        if (maxTokens <= 0) return;
        var session = sessions.active(restaurantId);
        if (session == null || session.issuedCount() < session.maxTokens()) return;
        Instant now = clock.instant();
        activeItems(restaurantId).forEach(item -> {
            item.setAvailable(false);
            item.setDeletedAt(now);
        });
        items.flush();
        sessions.closeIfFull(restaurantId);
    }

    private MenuCategory sessionCategory(UUID restaurantId) {
        return categories.findByRestaurantIdAndDeletedAtIsNull(restaurantId, Pageable.unpaged()).stream()
                .filter(c -> SESSION_CATEGORY.equalsIgnoreCase(c.getName()))
                .findFirst().orElse(null);
    }

    private List<MenuItem> activeItems(UUID restaurantId) {
        return items.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("restaurant").get("id"), restaurantId),
                cb.isNull(root.get("deletedAt")),
                cb.equal(root.get("category").get("name"), SESSION_CATEGORY)
        ), org.springframework.data.domain.Sort.by("displayOrder").and(org.springframework.data.domain.Sort.by("id")));
    }

    private SessionMenuItem view(MenuItem item) {
        return new SessionMenuItem(item.getId(), item.getVersion(), item.getName(), item.getBasePrice(), item.getImageUrl(),
                item.getPreparationTimeMinutes(), item.isAvailable());
    }
}
