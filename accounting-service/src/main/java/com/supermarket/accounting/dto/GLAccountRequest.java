package com.supermarket.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class GLAccountRequest {

    @NotBlank
    private String accountCode;

    @NotBlank
    private String accountName;

    private String description;

    @NotBlank
    private String accountType;

    @NotBlank
    private String balanceType;

    private String parentAccountCode;

    private boolean isActive = true;

    private boolean isCashFlow = false;

    private int level = 1;

    private BigDecimal openingBalance = BigDecimal.ZERO;

    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getBalanceType() { return balanceType; }
    public void setBalanceType(String balanceType) { this.balanceType = balanceType; }
    public String getParentAccountCode() { return parentAccountCode; }
    public void setParentAccountCode(String parentAccountCode) { this.parentAccountCode = parentAccountCode; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public boolean isCashFlow() { return isCashFlow; }
    public void setCashFlow(boolean cashFlow) { isCashFlow = cashFlow; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
}
