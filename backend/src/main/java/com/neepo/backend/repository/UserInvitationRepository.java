package com.neepo.backend.repository;

import com.neepo.backend.entity.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {
    Optional<UserInvitation> findByTokenHash(String tokenHash);
    List<UserInvitation> findAllByCompanyId(UUID companyId);
    List<UserInvitation> findAllByUserId(UUID userId);
    Optional<UserInvitation> findFirstByUserIdAndAcceptedAtIsNullOrderByCreatedAtDesc(UUID userId);
    Optional<UserInvitation> findByIdAndCompanyId(UUID id, UUID companyId);
    void deleteAllByUserId(UUID userId);
}
