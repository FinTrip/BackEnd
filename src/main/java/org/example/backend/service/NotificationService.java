package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.NotificationRespone;
import org.example.backend.entity.Notification;
import org.example.backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service

public class NotificationService {
    private final NotificationRepository notificationRepository;
    public Notification createNotification(NotificationRespone notificationRespone) {
        Notification notification = new Notification();
        notification.setMessage(notificationRespone.getMessage());
        notification.setIsRead(false);
        return notificationRepository.save(notification);
    }
}
