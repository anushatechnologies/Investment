package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserIdOrderBySentAtDesc(String userId);
    long countByUserIdAndReadFlagFalse(String userId);
    Optional<Notification> findByIdAndUserId(String id, String userId);
}
