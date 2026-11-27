package com.bookeat.order.mapper;
import com.bookeat.order.dto.OrderResponses.*;
import com.bookeat.order.entity.*;
import com.bookeat.order.repository.*;
import com.bookeat.customer.repository.CustomerRepository;
import com.bookeat.menu.repository.MenuItemRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
@Component @RequiredArgsConstructor
public class OrderMapper {
    private final FoodOrderItemRepository items;private final FoodOrderItemAddonRepository addons;private final OrderStatusHistoryRepository history;private final CustomerRepository customers;private final MenuItemRepository menuItems;
    public Order order(FoodOrder o){return new Order(o.getId(),o.getVersion(),o.getOrderNumber(),o.getRestaurantId(),o.getReservationId(),o.getGroupBookingId(),o.getCorporateBookingId(),o.getTakeawayOrderId(),o.getDiniTicketId(),o.getOrderType(),o.getStatus(),o.getSubtotal(),o.getDiscount(),o.getTaxPercent(),o.getTax(),o.getTotal(),o.getCurrency(),o.getSpecialInstructions(),customerName(o),customerPhone(o),items.findByOrderIdOrderByCreatedAtAscIdAsc(o.getId()).stream().map(i->new Line(i.getId(),i.getMenuItemId(),i.getItemName(),i.getImageUrl()!=null&&!i.getImageUrl().isBlank()?i.getImageUrl():(i.getMenuItemId()==null?null:menuItems.findById(i.getMenuItemId()).map(m->m.getImageUrl()).orElse(null)),i.getVariantId(),i.getVariantName(),i.getQuantity(),i.getBaseUnitPrice(),i.getUnitPrice(),i.getSubtotal(),i.getNotes(),addons.findByOrderItemIdOrderByCreatedAtAscIdAsc(i.getId()).stream().map(a->new Addon(a.getMenuAddonId(),a.getName(),a.getPrice())).toList())).toList(),history.findByOrderIdOrderByChangedAtAscIdAsc(o.getId()).stream().map(h->new History(h.getOldStatus(),h.getNewStatus(),h.getReason(),h.getChangedAt())).toList(),o.getCreatedAt());}
    private String customerName(FoodOrder o){return customers.findById(o.getCustomerId()).map(c->{var u=c.getUser();var n=((u.getFirstName()==null?"":u.getFirstName())+" "+(u.getLastName()==null?"":u.getLastName())).trim();return n.isEmpty()?"Customer":n;}).orElse("Customer");}
    private String customerPhone(FoodOrder o){return customers.findById(o.getCustomerId()).map(c->c.getUser().getPhoneNumber()).orElse(null);}
}
