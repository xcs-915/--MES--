package com.itrm.platform.repo;

import com.itrm.platform.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByRecipientOrderByCreatedAtDesc(String recipient);
    long countByRecipientAndReadFlagFalse(String recipient);
}
