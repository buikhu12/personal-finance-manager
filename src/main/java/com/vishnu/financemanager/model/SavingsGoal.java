package com.vishnu.financemanager.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String goalName;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal targetAmount;

    @Column(precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Column(nullable = false)
    private LocalDate startDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
