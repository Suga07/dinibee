package com.bookeat.reservation.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="corporate_accounts") @Getter @Setter @NoArgsConstructor
public class CorporateAccount extends BaseEntity {
    @Column(nullable=false) private UUID ownerCustomerId;
    @Column(nullable=false,length=160) private String companyName;
    @Column(nullable=true,length=30) private String gstNumber;
    @Column(nullable=false,length=1000) private String billingAddress;
    @Column(nullable=false,length=100) private String contactPersonName;
    @Column(nullable=false,length=320) private String contactEmail;
    @Column(nullable=false,length=30) private String contactPhone;
    @Column(nullable=false) private boolean active;
}
