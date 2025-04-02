package org.example.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRespone {
    private int id;
    private String message;
    private Boolean isRead;
    private Integer userId;
}
