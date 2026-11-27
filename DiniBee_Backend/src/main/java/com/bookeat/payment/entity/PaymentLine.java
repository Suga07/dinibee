package com.bookeat.payment.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="payment_lines") @Getter @Setter @NoArgsConstructor
public class PaymentLine extends BaseEntity {
    @Column(nullable=false) private UUID paymentId;
    @Column(nullable=false) private UUID foodOrderId;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
}
