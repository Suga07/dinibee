package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="diniticket_settings")
public class DiniTicketSettings extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false,unique=true)
    private Restaurant restaurant;
    @Column(nullable=false)
    private boolean enabled;
    @Column(nullable=false)
    private int maximumQueueSize;
    @Column(nullable=false)
    private int defaultEstimatedMinutesPerParty;
    @Column(nullable=false)
    private boolean allowPreOrder;
    @Column(nullable=false)
    private boolean allowRemoteJoin;
    @Column(nullable=false)
    private int ticketExpiryMinutes;
    @Column(nullable=false)
    private boolean autoExpireTickets;
}
