package com.bookeat.order.service;
import com.bookeat.order.repository.RestaurantOrderSettingsRepository;
import com.bookeat.order.entity.RestaurantOrderSettings;
import com.bookeat.restaurant.service.*;
import com.bookeat.common.response.RestaurantResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional
public class OrderSettingsService {
    private final RestaurantOrderSettingsRepository settings;private final RestaurantInventoryAccess inventory;private final RestaurantAccessService access;private final RestaurantChangeService changes;
    public RestaurantResource<BigDecimal> update(UUID actor,UUID id,long version,BigDecimal tax){var r=inventory.lockEditable(actor,id,version);var s=settings.findByRestaurantId(id).orElseGet(RestaurantOrderSettings::new);s.setRestaurantId(id);s.setTaxPercent(tax);settings.save(s);changes.finish(r,actor,"ORDER_TAX_POLICY_UPDATED");return new RestaurantResource<>(r.getVersion(),tax);}
    @Transactional(readOnly=true) public RestaurantResource<BigDecimal> get(UUID actor,UUID id){var r=access.getOwnedRestaurant(actor,id);return new RestaurantResource<>(r.getVersion(),settings.findByRestaurantId(id).map(RestaurantOrderSettings::getTaxPercent).orElse(BigDecimal.ZERO));}
}
