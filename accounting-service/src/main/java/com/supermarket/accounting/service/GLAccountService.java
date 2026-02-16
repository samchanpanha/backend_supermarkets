package com.supermarket.accounting.service;

import com.supermarket.accounting.dto.GLAccountRequest;
import com.supermarket.accounting.dto.GLAccountResponse;
import com.supermarket.accounting.entity.GLAccount;
import com.supermarket.accounting.repository.GLAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GLAccountService {

    private final GLAccountRepository glAccountRepository;

    public GLAccountService(GLAccountRepository glAccountRepository) {
        this.glAccountRepository = glAccountRepository;
    }

    public GLAccountResponse createAccount(GLAccountRequest request, String tenantId) {
        if (glAccountRepository.existsByAccountCodeAndTenantId(request.getAccountCode(), tenantId)) {
            throw new RuntimeException("GL Account with code " + request.getAccountCode() + " already exists");
        }

        GLAccount account = new GLAccount();
        account.setTenantId(tenantId);
        account.setAccountCode(request.getAccountCode());
        account.setAccountName(request.getAccountName());
        account.setDescription(request.getDescription());
        account.setAccountType(request.getAccountType());
        account.setBalanceType(request.getBalanceType());
        account.setParentAccountCode(request.getParentAccountCode());
        account.setIsActive(request.isActive());
        account.setIsCashFlow(request.isCashFlow());
        account.setLevel(request.getLevel());
        account.setOpeningBalance(request.getOpeningBalance());
        account.setCurrentBalance(request.getOpeningBalance());

        GLAccount saved = glAccountRepository.save(account);
        return mapToResponse(saved);
    }

    public GLAccountResponse updateAccount(Long id, GLAccountRequest request, String tenantId) {
        GLAccount account = glAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("GL Account not found"));

        if (!account.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to account");
        }

        account.setAccountName(request.getAccountName());
        account.setDescription(request.getDescription());
        account.setAccountType(request.getAccountType());
        account.setBalanceType(request.getBalanceType());
        account.setParentAccountCode(request.getParentAccountCode());
        account.setIsActive(request.isActive());
        account.setIsCashFlow(request.isCashFlow());
        account.setLevel(request.getLevel());

        GLAccount saved = glAccountRepository.save(account);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public GLAccountResponse getAccountById(Long id, String tenantId) {
        GLAccount account = glAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("GL Account not found"));

        if (!account.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to account");
        }

        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public GLAccountResponse getAccountByCode(String accountCode, String tenantId) {
        GLAccount account = glAccountRepository.findByAccountCodeAndTenantId(accountCode, tenantId)
                .orElseThrow(() -> new RuntimeException("GL Account not found"));
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public List<GLAccountResponse> getAllAccounts(String tenantId) {
        return glAccountRepository.findByTenantId(tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GLAccountResponse> getAccountsByType(String tenantId, String accountType) {
        return glAccountRepository.findByTenantIdAndAccountType(tenantId, accountType)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void updateAccountBalance(String accountCode, String tenantId, java.math.BigDecimal amount, boolean isDebit) {
        GLAccount account = glAccountRepository.findByAccountCodeAndTenantId(accountCode, tenantId)
                .orElseThrow(() -> new RuntimeException("GL Account not found"));

        if ("DEBIT".equals(account.getBalanceType())) {
            if (isDebit) {
                account.setCurrentBalance(account.getCurrentBalance().add(amount));
            } else {
                account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
            }
        } else {
            if (isDebit) {
                account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
            } else {
                account.setCurrentBalance(account.getCurrentBalance().add(amount));
            }
        }

        glAccountRepository.save(account);
    }

    private GLAccountResponse mapToResponse(GLAccount account) {
        GLAccountResponse response = new GLAccountResponse();
        response.setId(account.getId());
        response.setTenantId(account.getTenantId());
        response.setAccountCode(account.getAccountCode());
        response.setAccountName(account.getAccountName());
        response.setDescription(account.getDescription());
        response.setAccountType(account.getAccountType());
        response.setBalanceType(account.getBalanceType());
        response.setParentAccountCode(account.getParentAccountCode());
        response.setActive(account.isActive());
        response.setCashFlow(account.isCashFlow());
        response.setLevel(account.getLevel());
        response.setOpeningBalance(account.getOpeningBalance());
        response.setCurrentBalance(account.getCurrentBalance());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        return response;
    }
}
