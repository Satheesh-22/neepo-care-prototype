package com.neepo.backend.repository;

import com.neepo.backend.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, UUID> {

    List<NotificationChannel> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<NotificationChannel> findByCompanyIdAndActiveTrue(UUID companyId);

    Optional<NotificationChannel> findByIdAndCompanyId(UUID id, UUID companyId);
}
