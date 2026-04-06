package com.financeapp.finance_backend;

import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeapp.finance_backend.auth.repository.RefreshTokenRepository;
import com.financeapp.finance_backend.audit.repository.AuditLogRepository;
import com.financeapp.finance_backend.category.repository.CategoryRepository;
import com.financeapp.finance_backend.record.repository.FinancialRecordRepository;
import com.financeapp.finance_backend.security.jwt.JwtProperties;
import com.financeapp.finance_backend.security.jwt.JwtTokenProvider;
import com.financeapp.finance_backend.user.entity.User;
import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import com.financeapp.finance_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("finance_test")
                .withUsername("test_user")
                .withPassword("test_pass");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;
    @Autowired
    protected AuditLogRepository auditLogRepository;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected FinancialRecordRepository recordRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JwtTokenProvider jwtTokenProvider;
    @Autowired
    protected JwtProperties jwtProperties;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;

    protected User testAdmin;
    protected User testAnalyst;
    protected User testViewer;
    protected String adminToken;
    protected String analystToken;
    protected String viewerToken;

    @BeforeEach
    void setUpBase() {
        new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                .execute(status -> {
                    entityManager.createNativeQuery(
                            "TRUNCATE TABLE financial_records, categories, refresh_tokens, audit_logs, users RESTART IDENTITY CASCADE")
                            .executeUpdate();
                    return null;
                });

        testAdmin = createUser("admin@test.com", UserRole.ADMIN, UserStatus.ACTIVE);
        testAnalyst = createUser("analyst@test.com", UserRole.ANALYST, UserStatus.ACTIVE);
        testViewer = createUser("viewer@test.com", UserRole.VIEWER, UserStatus.ACTIVE);

        adminToken = generateToken(testAdmin);
        analystToken = generateToken(testAnalyst);
        viewerToken = generateToken(testViewer);
    }

    @BeforeEach
    void resetRateLimitCache() {
        if (cacheManager != null) {
            cacheManager.getCacheNames()
                    .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
        }
    }

    protected User createUser(String email, UserRole role, UserStatus status) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1!"))
                .fullName("Test " + role.name())
                .role(role)
                .status(status)
                .build();
        return userRepository.save(user);
    }

    protected String generateToken(User user) {
        return jwtTokenProvider.generateToken(user.getId(), user.getRole().name(), "test-fp");
    }

    protected String bearerToken(String token) {
        return "Bearer " + token;
    }

    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}