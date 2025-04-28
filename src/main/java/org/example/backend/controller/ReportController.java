package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.AdminActionRequest;
import org.example.backend.dto.ApiResponse;
import org.example.backend.dto.ReportRequest;
import org.example.backend.entity.Report;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {
    private final ReportService reportService;
    
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReport(
            HttpServletRequest request,
            @Valid @RequestBody ReportRequest reportRequest) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            Report report = reportService.createReport(userEmail, reportRequest);
            log.info("Report created with ID: {}", report.getId());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", report.getId());
            responseData.put("type", report.getReportType());
            responseData.put("reason", report.getReason());
            responseData.put("status", report.getStatus());
            responseData.put("createdAt", report.getCreatedAt());
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating report", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/admin/pending")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingReports(
            HttpServletRequest request) {
        try {
            String adminEmail = (String) request.getAttribute("userEmail");
            if (adminEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            // Danh sách báo cáo đang chờ xử lý
            List<Report> pendingReports = reportService.getAllPendingReports();
            
            List<Map<String, Object>> responseData = formatReportList(pendingReports);
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting pending reports", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/admin/process/{reportId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processReport(
            HttpServletRequest request,
            @PathVariable Integer reportId,
            @Valid @RequestBody AdminActionRequest actionRequest) {
        try {
            String adminEmail = (String) request.getAttribute("userEmail");
            if (adminEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            Report processedReport = reportService.processReport(adminEmail, reportId, actionRequest);
            log.info("Report processed: {}", processedReport.getId());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", processedReport.getId());
            responseData.put("type", processedReport.getReportType());
            responseData.put("status", processedReport.getStatus());
            responseData.put("adminNote", processedReport.getAdminNote());
            responseData.put("processedAt", processedReport.getProcessedAt());
            
            if (processedReport.getReportType() == Report.ReportType.USER_REPORT) {
                responseData.put("reportedUserId", processedReport.getReportedUser().getId());
                responseData.put("reportedUserEmail", processedReport.getReportedUser().getEmail());
                responseData.put("reportedUserStatus", processedReport.getReportedUser().getStatus());
            } else if (processedReport.getReportType() == Report.ReportType.POST_REPORT) {
                if (processedReport.getReportedPost() != null) {
                    responseData.put("postExists", true);
                    responseData.put("reportedPostId", processedReport.getReportedPost().getId());
                    responseData.put("reportedPostStatus", processedReport.getReportedPost().getStatus());
                } else {
                    responseData.put("postExists", false);
                    responseData.put("message", "Post has been deleted");
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing report", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/admin/user-reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserReports(
            HttpServletRequest request) {
        try {
            String adminEmail = (String) request.getAttribute("userEmail");
            if (adminEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            List<Report> userReports = reportService.getReportsByType(Report.ReportType.USER_REPORT);
            List<Map<String, Object>> responseData = formatReportList(userReports);
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting user reports", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/admin/post-reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPostReports(
            HttpServletRequest request) {
        try {
            String adminEmail = (String) request.getAttribute("userEmail");
            if (adminEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            List<Report> postReports = reportService.getReportsByType(Report.ReportType.POST_REPORT);
            List<Map<String, Object>> responseData = formatReportList(postReports);
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting post reports", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Hàm định dạng danh sách báo cáo
    private List<Map<String, Object>> formatReportList(List<Report> reports) {
        return reports.stream()
                .map(report -> {
                    Map<String, Object> reportMap = new HashMap<>();
                    reportMap.put("id", report.getId());
                    reportMap.put("type", report.getReportType());
                    reportMap.put("reason", report.getReason());
                    reportMap.put("status", report.getStatus());
                    reportMap.put("createdAt", report.getCreatedAt());
                    
                    // Thông tin người báo cáo
                    Map<String, Object> reporterMap = new HashMap<>();
                    reporterMap.put("id", report.getReporter().getId());
                    reporterMap.put("email", report.getReporter().getEmail());
                    reporterMap.put("fullName", report.getReporter().getFullName());
                    reportMap.put("reporter", reporterMap);
                    
                    // Thông tin tùy thuộc vào loại báo cáo
                    if (report.getReportType() == Report.ReportType.USER_REPORT && report.getReportedUser() != null) {
                        Map<String, Object> reportedUserMap = new HashMap<>();
                        reportedUserMap.put("id", report.getReportedUser().getId());
                        reportedUserMap.put("email", report.getReportedUser().getEmail());
                        reportedUserMap.put("fullName", report.getReportedUser().getFullName());
                        reportedUserMap.put("status", report.getReportedUser().getStatus());
                        reportMap.put("reportedUser", reportedUserMap);
                    } else if (report.getReportType() == Report.ReportType.POST_REPORT && report.getReportedPost() != null) {
                        Map<String, Object> reportedPostMap = new HashMap<>();
                        reportedPostMap.put("id", report.getReportedPost().getId());
                        reportedPostMap.put("title", report.getReportedPost().getTitle());
                        
                        // Thông tin tác giả bài viết
                        Map<String, Object> authorMap = new HashMap<>();
                        authorMap.put("id", report.getReportedPost().getUser().getId());
                        authorMap.put("email", report.getReportedPost().getUser().getEmail());
                        authorMap.put("fullName", report.getReportedPost().getUser().getFullName());
                        reportedPostMap.put("author", authorMap);
                        
                        reportMap.put("reportedPost", reportedPostMap);
                    }
                    
                    // Thông tin xử lý báo cáo
                    if (report.getProcessedBy() != null) {
                        Map<String, Object> processedByMap = new HashMap<>();
                        processedByMap.put("id", report.getProcessedBy().getId());
                        processedByMap.put("email", report.getProcessedBy().getEmail());
                        processedByMap.put("fullName", report.getProcessedBy().getFullName());
                        reportMap.put("processedBy", processedByMap);
                        reportMap.put("processedAt", report.getProcessedAt());
                        reportMap.put("adminNote", report.getAdminNote());
                    }
                    
                    return reportMap;
                })
                .collect(Collectors.toList());
    }
} 