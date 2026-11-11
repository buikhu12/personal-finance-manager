package com.vishnu.financemanager.controller;

import com.vishnu.financemanager.model.User;
import com.vishnu.financemanager.model.Wallet;
import com.vishnu.financemanager.repository.UserRepository;
import com.vishnu.financemanager.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wallet-bank-account-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class WalletBankAccountIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private WalletRepository walletRepo;

    @Test
    void bankDetailsAreValidatedAndEditableOnlyByTheWalletOwner() throws Exception {
        User owner = createUser("wallet-owner@example.com");
        User other = createUser("wallet-other@example.com");
        Wallet wallet = walletRepo.save(Wallet.builder()
                .user(owner)
                .name("Test wallet")
                .initialBalance(new BigDecimal("300000"))
                .build());
        String walletPath = "/api/wallets/" + wallet.getId();
        String bankPath = walletPath + "/bank-account";
        String bankDetails = """
                {"bankName":" Vietcombank ","lastFour":"0042"}
                """;

        mvc.perform(put(bankPath).with(user(other.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON).content(bankDetails))
                .andExpect(status().isBadRequest());
        assertThat(walletRepo.findById(wallet.getId()).orElseThrow().getBankName()).isNull();

        mvc.perform(put(bankPath).with(user(owner.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bankName":"Vietcombank","lastFour":"123456789"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.lastFour").exists());

        mvc.perform(put(bankPath).with(user(owner.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON).content(bankDetails))
                .andExpect(status().isNoContent());

        mvc.perform(put(walletPath).with(user(owner.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed wallet","initialBalance":300000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankName").value("Vietcombank"))
                .andExpect(jsonPath("$.bankAccountLastFour").value("0042"));

        mvc.perform(delete(bankPath).with(user(owner.getUsername())))
                .andExpect(status().isNoContent());

        Wallet updated = walletRepo.findById(wallet.getId()).orElseThrow();
        assertThat(updated.getBankName()).isNull();
        assertThat(updated.getBankAccountLastFour()).isNull();
        assertThat(updated.getName()).isEqualTo("Renamed wallet");
        assertThat(updated.getInitialBalance()).isEqualByComparingTo("300000");
    }

    private User createUser(String username) {
        return userRepo.save(User.builder()
                .username(username)
                .password("unused")
                .fullName("Wallet Test User")
                .phoneNumber("0000000000")
                .build());
    }
}
