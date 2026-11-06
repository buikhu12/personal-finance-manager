package com.vishnu.financemanager.controller;

import com.vishnu.financemanager.dto.*;
import com.vishnu.financemanager.service.SavingsGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class SavingsGoalController {

    private final SavingsGoalService service;

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody GoalRequest request, Principal principal) {
        return new ResponseEntity<>(service.createGoal(request, principal), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getAll(Principal principal) {
        return ResponseEntity.ok(service.getAll(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getOne(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(service.getOne(id, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody GoalRequest request, Principal principal) {
        return ResponseEntity.ok(service.update(id, request, principal));
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<GoalResponse> contribute(@PathVariable Long id,
                                                   @RequestParam BigDecimal amount,
                                                   Principal principal) {
        return ResponseEntity.ok(service.addSavings(id, amount, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id, Principal principal) {
        service.delete(id, principal);
        return ResponseEntity.ok(Map.of("message", "Goal deleted successfully"));
    }
}
