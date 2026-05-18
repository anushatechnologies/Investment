package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserIdOrderBySentAtDesc(String userId);
}
