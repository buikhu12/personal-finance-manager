package com.vishnu.financemanager.controller;

import com.vishnu.financemanager.dto.TransactionRequest;
import com.vishnu.financemanager.dto.TransactionResponse;
import com.vishnu.financemanager.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request, Principal principal) {
        return new ResponseEntity<>(service.create(request, principal), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll(Principal principal) {
        return ResponseEntity.ok(service.getAll(principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody TransactionRequest request,
                                                      Principal principal) {
        return ResponseEntity.ok(service.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id, Principal principal) {
        service.delete(id, principal);
        return ResponseEntity.ok(Map.of("message", "Transaction deleted successfully"));
    }
}
