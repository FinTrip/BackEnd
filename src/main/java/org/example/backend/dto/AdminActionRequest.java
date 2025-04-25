package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.backend.entity.Report;

@Data
public class AdminActionRequest {
    @NotNull(message = "Trạng thái báo cáo không được để trống")
    private Report.ReportStatus status;

    @NotBlank(message = "Ghi chú không được để trống")
    private String adminNote;
    
    // true nếu muốn xóa bài viết hoặc khóa tài khoản
    private boolean takeAction;
} 