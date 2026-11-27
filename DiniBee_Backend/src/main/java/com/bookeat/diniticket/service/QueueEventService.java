package com.bookeat.diniticket.service;
import com.bookeat.diniticket.repository.QueueEventRepository;
import com.bookeat.commerce.service.CommerceAccess;
import com.bookeat.restaurant.service.RestaurantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class QueueEventService {
    private final QueueEventRepository events;private final CommerceAccess access;private final RestaurantAccessService owners;
    public List<QueueEventRepository.Event> customer(UUID actor,long after){return events.customer(access.customer(actor),after);}
    public List<QueueEventRepository.Event> owner(UUID actor,UUID restaurant,long after){owners.getOwnedRestaurant(actor,restaurant);return events.owner(restaurant,after);}
}
