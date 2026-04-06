package com.financeapp.finance_backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordGeneratorTest {

    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String rawPassword = "Password1!";
        String hashedPassword = encoder.encode(rawPassword);
        
        System.out.println("\n========================================");
        System.out.println("YOUR HASH: " + hashedPassword);
        System.out.println("========================================\n");
    }
}