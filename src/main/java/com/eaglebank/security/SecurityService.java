package com.eaglebank.security;

import com.eaglebank.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for authorization checks used with @PreAuthorize annotations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityService {

    private final BankAccountRepository bankAccountRepository;

    public boolean isOwner(String userId) {
        String currentUserId = getCurrentUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(userId);
        log.debug("Authorization check - isOwner: userId={}, currentUserId={}, result={}",
                userId, currentUserId, isOwner);
        return isOwner;
    }

    public boolean ownsAccount(String accountNumber) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            log.debug("Authorization check - ownsAccount: No authenticated user");
            return false;
        }

        boolean owns = bankAccountRepository.findByAccountNumber(accountNumber)
                .map(account -> account.getUser().getUserId().equals(currentUserId))
                .orElse(true);

        log.debug("Authorization check - ownsAccount: accountNumber={}, currentUserId={}, result={}",
                accountNumber, currentUserId, owns);
        return owns;
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }
}

