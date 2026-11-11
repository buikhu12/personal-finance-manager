package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.WalletRequest;
import com.vishnu.financemanager.dto.WalletResponse;
import com.vishnu.financemanager.model.Category;
import com.vishnu.financemanager.model.CategoryType;
import com.vishnu.financemanager.model.Transaction;
import com.vishnu.financemanager.model.User;
import com.vishnu.financemanager.model.Wallet;
import com.vishnu.financemanager.repository.TransactionRepository;
import com.vishnu.financemanager.repository.UserRepository;
import com.vishnu.financemanager.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class WalletServiceTest {

    @InjectMocks
    private WalletService service;

    @Mock
    private WalletRepository walletRepo;

    @Mock
    private TransactionRepository transactionRepo;

    @Mock
    private UserRepository userRepo;

    private Principal principal;
    private User user;
    private Wallet defaultWallet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        principal = () -> "test@example.com";
        user = User.builder().id(1L).username("test@example.com").build();
        defaultWallet = Wallet.builder()
                .id(1L)
                .name(WalletService.DEFAULT_WALLET_NAME)
                .initialBalance(new BigDecimal("100000"))
                .user(user)
                .build();

        when(userRepo.findByUsername("test@example.com")).thenReturn(Optional.of(user));
        when(walletRepo.findByNameIgnoreCaseAndUser(WalletService.DEFAULT_WALLET_NAME, user))
                .thenReturn(Optional.of(defaultWallet));
    }

    @Test
    void getAll_calculatesCurrentBalanceFromInitialBalanceAndTransactions() {
        Category income = Category.builder().name("Salary").type(CategoryType.INCOME).build();
        Category expense = Category.builder().name("Food").type(CategoryType.EXPENSE).build();
        Transaction salary = Transaction.builder()
                .amount(new BigDecimal("500000"))
                .date(LocalDate.now())
                .category(income)
                .wallet(defaultWallet)
                .user(user)
                .build();
        Transaction food = Transaction.builder()
                .amount(new BigDecimal("120000"))
                .date(LocalDate.now())
                .category(expense)
                .wallet(defaultWallet)
                .user(user)
                .build();

        when(walletRepo.findByUserOrderByNameAsc(user)).thenReturn(List.of(defaultWallet));
        when(transactionRepo.findByUser(user)).thenReturn(List.of(salary, food));

        List<WalletResponse> result = service.getAll(principal);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("480000"), result.get(0).getCurrentBalance());
        assertEquals(2, result.get(0).getTransactionCount());
    }

    @Test
    void create_duplicateName_throwsException() {
        WalletRequest request = new WalletRequest();
        request.setName("Ví mặc định");
        request.setInitialBalance(BigDecimal.ZERO);
        when(walletRepo.existsByNameIgnoreCaseAndUser("Ví mặc định", user)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create(request, principal));
    }
}
