package com.vishnu.financemanager.controller;

import com.vishnu.financemanager.dto.BudgetRequest;
import com.vishnu.financemanager.dto.BudgetResponse;
import com.vishnu.financemanager.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService service;

    @GetMapping
    public List<BudgetResponse> getAll(@RequestParam(defaultValue = "") String month, Principal principal) {
        return service.getAll(month, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@Valid @RequestBody BudgetRequest request, Principal principal) {
        return service.create(request, principal);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(@PathVariable Long id, @Valid @RequestBody BudgetRequest request, Principal principal) {
        return service.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) {
        service.delete(id, principal);
    }
}
