package com.supermarket.pos.service;

import com.supermarket.pos.entity.POSTerminal;
import com.supermarket.pos.repository.POSTerminalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class POSTerminalService {

    private final POSTerminalRepository posTerminalRepository;

    public POSTerminalService(POSTerminalRepository posTerminalRepository) {
        this.posTerminalRepository = posTerminalRepository;
    }

    public POSTerminal createTerminal(POSTerminal terminal, String tenantId) {
        terminal.setTenantId(tenantId);
        terminal.setStatus("ACTIVE");
        return posTerminalRepository.save(terminal);
    }

    @Transactional(readOnly = true)
    public POSTerminal getTerminalByCode(String terminalCode, String tenantId) {
        return posTerminalRepository.findByTerminalCodeAndTenantId(terminalCode, tenantId)
                .orElseThrow(() -> new RuntimeException("Terminal not found"));
    }

    @Transactional(readOnly = true)
    public List<POSTerminal> getAllTerminals(String tenantId) {
        return posTerminalRepository.findAll().stream()
                .filter(t -> t.getTenantId().equals(tenantId))
                .collect(Collectors.toList());
    }

    public void updateTerminalStats(String terminalCode, String tenantId, int transactionCount, java.math.BigDecimal amount) {
        POSTerminal terminal = getTerminalByCode(terminalCode, tenantId);
        terminal.setTotalTransactions(terminal.getTotalTransactions() + transactionCount);
        terminal.setTotalAmount(terminal.getTotalAmount().add(amount));
        terminal.setLastTransactionAt(LocalDateTime.now());
        posTerminalRepository.save(terminal);
    }
}
