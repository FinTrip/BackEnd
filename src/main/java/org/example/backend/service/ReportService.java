package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.AdminActionRequest;
import org.example.backend.dto.ReportRequest;
import org.example.backend.entity.BlogPost;
import org.example.backend.entity.Report;
import org.example.backend.entity.User;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.repository.BlogPostRepository;
import org.example.backend.repository.ReportRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final BlogPostRepository blogPostRepository;
    private final NotificationService notificationService;
    
    @Transactional
    public Report createReport(String reporterEmail, ReportRequest request) {
        // Tìm người báo cáo
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
                
        Report report = new Report();
        report.setReporter(reporter);
        report.setReportType(request.getReportType());
        report.setReason(request.getReason());
        report.setStatus(Report.ReportStatus.PENDING);
        
        // Xử lý dựa vào loại báo cáo
        if (request.getReportType() == Report.ReportType.USER_REPORT) {
            if (request.getReportedUserId() == null) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Reported user ID is required for user reports");
            }
            
            // Tìm người dùng bị báo cáo
            User reportedUser = userRepository.findById(request.getReportedUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Reported user not found"));
                    
            // Không thể tự báo cáo chính mình
            if (reporter.getId().equals(reportedUser.getId())) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Cannot report yourself");
            }
            
            report.setReportedUser(reportedUser);
            log.info("User {} reported user {}", reporter.getId(), reportedUser.getId());
            
        } else if (request.getReportType() == Report.ReportType.POST_REPORT) {
            if (request.getReportedPostId() == null) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Reported post ID is required for post reports");
            }
            
            // Tìm bài viết bị báo cáo
            BlogPost reportedPost = blogPostRepository.findById(request.getReportedPostId())
                    .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND, "Reported post not found"));
                    
            // Không thể báo cáo bài viết của chính mình
            if (reporter.getId().equals(reportedPost.getUser().getId())) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Cannot report your own post");
            }
            
            report.setReportedPost(reportedPost);
            log.info("User {} reported post {}", reporter.getId(), reportedPost.getId());
        }
        
        return reportRepository.save(report);
    }
    
    public List<Report> getAllPendingReports() {
        return reportRepository.findByStatusOrderByCreatedAtDesc(Report.ReportStatus.PENDING);
    }
    
    public List<Report> getReportsByType(Report.ReportType reportType) {
        return reportRepository.findByReportTypeOrderByCreatedAtDesc(reportType);
    }
    
    public List<Report> getPendingReportsByType(Report.ReportType reportType) {
        return reportRepository.findByReportTypeAndStatusOrderByCreatedAtDesc(reportType, Report.ReportStatus.PENDING);
    }
    
    @Transactional
    public Report processReport(String adminEmail, Integer reportId, AdminActionRequest request) {
        // Tìm admin
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Admin not found"));
                
        // Kiểm tra quyền admin
        if (admin.getRole() == null || !admin.getRole().getRoleName().equalsIgnoreCase("ADMIN")) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS, "Only admins can process reports");
        }
        
        // Tìm báo cáo
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND, "Report not found"));
                
        // Cập nhật trạng thái báo cáo
        report.setStatus(request.getStatus());
        report.setAdminNote(request.getAdminNote());
        report.setProcessedBy(admin);
        report.setProcessedAt(LocalDateTime.now());
        
        log.info("Admin {} processed report {} with status {}", admin.getId(), reportId, request.getStatus());
        
        // Nếu được yêu cầu thực hiện hành động
        if (request.isTakeAction()) {
            // Xử lý với báo cáo người dùng
            if (report.getReportType() == Report.ReportType.USER_REPORT && report.getReportedUser() != null) {
                User reportedUser = report.getReportedUser();
                reportedUser.setStatus(User.UserStatus.banned);
                userRepository.save(reportedUser);
                log.info("Admin {} banned user {}", admin.getId(), reportedUser.getId());
            }
            
            // Xử lý với báo cáo bài viết
            if (report.getReportType() == Report.ReportType.POST_REPORT && report.getReportedPost() != null) {
                BlogPost postToUpdate = report.getReportedPost();
                
                // Lưu thông tin bài viết để debug (optional)
                Integer postId = postToUpdate.getId();
                
                // Thay đổi trạng thái bài viết thành ARCHIVED thay vì xóa
                try {
                    postToUpdate.setStatus(BlogPost.PostStatus.ARCHIVED);
                    blogPostRepository.save(postToUpdate);
                    log.info("Admin {} archived post {}", admin.getId(), postId);
                } catch (Exception e) {
                    log.error("Error archiving post {}: {}", postId, e.getMessage());
                    throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error archiving post: " + e.getMessage());
                }
            }
        }
        
        // Sau khi xử lý báo cáo, gửi email cho người liên quan
        if (report.getReportType() == Report.ReportType.USER_REPORT && report.getReportedUser() != null) {
            String toEmail = report.getReportedUser().getEmail();
            String subject = "Thông báo về báo cáo tài khoản";
            String content = """
        <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                <h2>Thông báo về xử lý tài khoản</h2>
                <p>Xin chào,</p>
                <p>Tài khoản của bạn đã được admin xử lý.</p>
                <p><strong>Ghi chú:</strong> %s</p>
                <p>Vui lòng liên hệ với bộ phận hỗ trợ nếu bạn cần thêm thông tin.</p>
                <br>
                <p>Trân trọng,</p>
                <p><em>Đội ngũ FinTrip</em></p>
            </body>
        </html>
        """.formatted(request.getAdminNote());

            notificationService.sendEmailWithHtml(toEmail, subject, content);

        } else if (report.getReportType() == Report.ReportType.POST_REPORT && report.getReportedPost() != null) {
            String toEmail = report.getReportedPost().getUser().getEmail();
            String subject = "Thông báo về báo cáo bài viết";
            String content = """
        <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                <h2>Thông báo về xử lý bài viết</h2>
                <p>Xin chào,</p>
                <p>Bài viết của bạn đã được admin xử lý.</p>
                <p>Bài viết đã được chuyển sang trạng thái Đã lưu trữ (Archived).</p>
                <p><strong>Ghi chú:</strong> %s</p>
                <p>Vui lòng liên hệ với bộ phận hỗ trợ nếu bạn cần thêm thông tin.</p>
                <br>
                <p>Trân trọng,</p>
                <p><em>Đội ngũ FinTrip</em></p>
            </body>
        </html>
        """.formatted(request.getAdminNote());

            notificationService.sendEmailWithHtml(toEmail, subject, content);
        }
        
        return reportRepository.save(report);
    }
} 