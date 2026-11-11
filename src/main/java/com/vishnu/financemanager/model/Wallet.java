package com.vishnu.financemanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@DynamicUpdate
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal initialBalance = BigDecimal.ZERO;

    @Column(length = 100)
    private String bankName;

    @Column(length = 4)
    private String bankAccountLastFour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
