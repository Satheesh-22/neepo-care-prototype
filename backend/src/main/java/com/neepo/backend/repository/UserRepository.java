package com.neepo.backend.repository;

import com.neepo.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findAllByCompanyId(UUID companyId, Pageable pageable);

    Optional<User> findByIdAndCompanyId(UUID id, UUID companyId);

    Optional<User> findByEmailIgnoreCaseAndCompanyId(String email, UUID companyId);
}
