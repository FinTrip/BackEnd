package org.example.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.ApiResponse;
import org.example.backend.service.VideoCallService;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/video-call")
@CrossOrigin
public class VideoCallController {

    @Autowired
    private VideoCallService videoCallService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, String>>> createRoom(
            HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            Map<String, String> response = videoCallService.createRoom(userEmail);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating video call room", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Map<String, String>>> joinRoom(
            HttpServletRequest request,
            @RequestParam String roomID) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }

            Map<String, String> response = videoCallService.joinRoom(roomID, userEmail);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error joining video call room", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/end")
    public ResponseEntity<ApiResponse<String>> endRoom(
            HttpServletRequest request,
            @RequestParam String roomID) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }

            String response = videoCallService.endRoom(roomID);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error ending video call room", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}

