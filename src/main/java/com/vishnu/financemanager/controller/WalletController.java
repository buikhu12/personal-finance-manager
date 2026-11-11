package com.vishnu.financemanager.controller;

import com.vishnu.financemanager.dto.WalletBankAccountRequest;
import com.vishnu.financemanager.dto.WalletRequest;
import com.vishnu.financemanager.dto.WalletResponse;
import com.vishnu.financemanager.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService service;

    @GetMapping
    public ResponseEntity<List<WalletResponse>> getAll(Principal principal) {
        return ResponseEntity.ok(service.getAll(principal));
    }

    @PostMapping
    public ResponseEntity<WalletResponse> create(@Valid @RequestBody WalletRequest request, Principal principal) {
        return new ResponseEntity<>(service.create(request, principal), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WalletResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody WalletRequest request,
                                                 Principal principal) {
        return ResponseEntity.ok(service.update(id, request, principal));
    }

    @PutMapping("/{id}/bank-account")
    public ResponseEntity<Void> linkBankAccount(@PathVariable Long id,
                                                @Valid @RequestBody WalletBankAccountRequest request,
                                                Principal principal) {
        service.linkBankAccount(id, request, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/bank-account")
    public ResponseEntity<Void> unlinkBankAccount(@PathVariable Long id, Principal principal) {
        service.unlinkBankAccount(id, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id, Principal principal) {
        service.delete(id, principal);
        return ResponseEntity.ok(Map.of("message", "Wallet deleted successfully"));
    }
}
