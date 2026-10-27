package com.neepo.backend.repository;

import com.neepo.backend.entity.NotificationDeliveryLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, UUID> {

    List<NotificationDeliveryLog> findByCompanyIdOrderByDeliveredAtDesc(UUID companyId, Pageable pageable);

    List<NotificationDeliveryLog> findByChannelIdOrderByDeliveredAtDesc(UUID channelId, Pageable pageable);
}
