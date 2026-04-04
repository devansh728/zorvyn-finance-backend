package com.financeapp.finance_backend.security.blacklist;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    // Simple in-memory string store for blacklisted tokens.
    // In a microservices or highly available environment, this would be Redis.
    private final Map<String, Boolean> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token) {
        blacklistedTokens.put(token, Boolean.TRUE);
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }
}
