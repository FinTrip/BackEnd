package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.backend.entity.Report;

@Data
public class ReportRequest {
    @NotNull(message = "Loại báo cáo không được để trống")
    private Report.ReportType reportType;
    
    // ID người dùng bị báo cáo (chỉ dùng khi báo cáo người dùng)
    private Integer reportedUserId;
    
    // ID bài viết bị báo cáo (chỉ dùng khi báo cáo bài viết)
    private Integer reportedPostId;
    
    @NotBlank(message = "Lý do báo cáo không được để trống")
    private String reason;
} 