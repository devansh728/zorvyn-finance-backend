package com.financeapp.finance_backend.user.repository;

import com.financeapp.finance_backend.user.entity.User;
import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ADMIN' AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    long countActiveAdmins();

    Page<User> findByDeletedAtIsNull(Pageable pageable);

    Page<User> findByRoleAndDeletedAtIsNull(UserRole role, Pageable pageable);

    Page<User> findByStatusAndDeletedAtIsNull(UserStatus status, Pageable pageable);

    Page<User> findByRoleAndStatusAndDeletedAtIsNull(UserRole role, UserStatus status, Pageable pageable);
}
