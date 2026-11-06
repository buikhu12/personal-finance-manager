package com.vishnu.financemanager.controller;

import com.vishnu.financemanager.dto.CategoryRequest;
import com.vishnu.financemanager.dto.CategoryResponse;
import com.vishnu.financemanager.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll(Principal principal) {
        return ResponseEntity.ok(service.getAllCategories(principal));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request, Principal principal) {
        return new ResponseEntity<>(service.create(request, principal), HttpStatus.CREATED);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String name, Principal principal) {
        service.delete(name, principal);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
    }
}
