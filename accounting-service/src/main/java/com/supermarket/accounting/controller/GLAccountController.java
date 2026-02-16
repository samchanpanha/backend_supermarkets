package com.supermarket.accounting.controller;

import com.supermarket.accounting.dto.GLAccountRequest;
import com.supermarket.accounting.dto.GLAccountResponse;
import com.supermarket.accounting.service.GLAccountService;
import com.supermarket.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounting/gl-accounts")
public class GLAccountController {

    private final GLAccountService glAccountService;

    public GLAccountController(GLAccountService glAccountService) {
        this.glAccountService = glAccountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GLAccountResponse>> createAccount(
            @Valid @RequestBody GLAccountRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        GLAccountResponse response = glAccountService.createAccount(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "GL Account created successfully", response, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GLAccountResponse>> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody GLAccountRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        GLAccountResponse response = glAccountService.updateAccount(id, request, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "GL Account updated successfully", response, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GLAccountResponse>> getAccount(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        GLAccountResponse response = glAccountService.getAccountById(id, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "GL Account retrieved successfully", response, null));
    }

    @GetMapping("/code/{accountCode}")
    public ResponseEntity<ApiResponse<GLAccountResponse>> getAccountByCode(
            @PathVariable String accountCode,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        GLAccountResponse response = glAccountService.getAccountByCode(accountCode, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "GL Account retrieved successfully", response, null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GLAccountResponse>>> getAllAccounts(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<GLAccountResponse> accounts = glAccountService.getAllAccounts(tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "GL Accounts retrieved successfully", accounts, null));
    }

    @GetMapping("/type/{accountType}")
    public ResponseEntity<ApiResponse<List<GLAccountResponse>>> getAccountsByType(
            @PathVariable String accountType,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<GLAccountResponse> accounts = glAccountService.getAccountsByType(tenantId, accountType);
        return ResponseEntity.ok(new ApiResponse<>(true, "GL Accounts retrieved successfully", accounts, null));
    }
}
