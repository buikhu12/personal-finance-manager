package com.vishnu.financemanager.controller;

import com.vishnu.financemanager.dto.*;
import com.vishnu.financemanager.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<MonthlyReportResponse> getMonthly(@PathVariable int year,
                                                            @PathVariable int month,
                                                            Principal principal) {
        return ResponseEntity.ok(service.getMonthly(year, month, principal));
    }

    @GetMapping("/yearly/{year}")
    public ResponseEntity<YearlyReportResponse> getYearly(@PathVariable int year, Principal principal) {
        return ResponseEntity.ok(service.getYearly(year, principal));
    }
}
