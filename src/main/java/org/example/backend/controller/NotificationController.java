package org.example.backend.controller;

import org.example.backend.dto.NotificationRespone;
import org.example.backend.entity.Notification;
import org.example.backend.service.NotificationService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/notification")
public class NotificationController {
    NotificationService notificationService;
    @MessageMapping("/notification")
    @SendTo("/user/notification")
    public NotificationRespone sendNotification(NotificationRespone notificationRespone) {
        Notification notification = notificationService.createNotification(notificationRespone);
        return notificationRespone;
    }

}
