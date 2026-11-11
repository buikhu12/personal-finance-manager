package com.vishnu.financemanager.repository;

import com.vishnu.financemanager.model.User;
import com.vishnu.financemanager.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByUserOrderByNameAsc(User user);
    Optional<Wallet> findByIdAndUser(Long id, User user);
    Optional<Wallet> findByNameIgnoreCaseAndUser(String name, User user);
    boolean existsByNameIgnoreCaseAndUser(String name, User user);

    @Modifying
    @Query("""
            UPDATE Wallet w
            SET w.bankName = :bankName, w.bankAccountLastFour = :lastFour
            WHERE w.id = :id AND w.user = :user
            """)
    int updateBankAccount(@Param("id") Long id, @Param("user") User user,
                          @Param("bankName") String bankName, @Param("lastFour") String lastFour);
}
