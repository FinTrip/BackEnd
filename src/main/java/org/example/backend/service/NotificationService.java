package org.example.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.NotificationRespone;
import org.example.backend.entity.Notification;
import org.example.backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

@RequiredArgsConstructor
@Slf4j
@Service
public class NotificationService {
    @Autowired
    private JavaMailSender javaMailSender;
    private final NotificationRepository notificationRepository;

    @Autowired
    private JavaMailSender mailSender;

    public Notification createNotification(NotificationRespone notificationRespone) {
        Notification notification = new Notification();
        notification.setMessage(notificationRespone.getMessage());
        notification.setIsRead(false);
        return notificationRepository.save(notification);
    }

//
public void sendEmailWithHtml(String to, String subject, String htmlContent) {
    MimeMessage message = javaMailSender.createMimeMessage();
    try {
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true => gửi HTML
        javaMailSender.send(message);
    } catch (MessagingException e) {
        // Xử lý lỗi
        e.printStackTrace();
    }
}
}
