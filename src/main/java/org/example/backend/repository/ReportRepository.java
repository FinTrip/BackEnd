package org.example.backend.repository;

import org.example.backend.entity.Report;
import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {
    List<Report> findByStatusOrderByCreatedAtDesc(Report.ReportStatus status);
    List<Report> findByReportTypeOrderByCreatedAtDesc(Report.ReportType reportType);
    List<Report> findByReporterOrderByCreatedAtDesc(User reporter);
    List<Report> findByReportedUserOrderByCreatedAtDesc(User reportedUser);
    List<Report> findByReportTypeAndStatusOrderByCreatedAtDesc(Report.ReportType reportType, Report.ReportStatus status);
} 