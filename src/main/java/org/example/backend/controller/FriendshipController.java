package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.ApiResponse;
import org.example.backend.dto.FriendDto;
import org.example.backend.dto.FriendshipRequest;
import org.example.backend.dto.FriendshipResponse;
import org.example.backend.dto.UserSearchResponse;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.service.FriendshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {
    private final FriendshipService friendshipService;
    
    /**
     * Gửi lời mời kết bạn
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendshipResponse>> sendFriendRequest(
            HttpServletRequest request,
            @Valid @RequestBody FriendshipRequest friendshipRequest) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            FriendshipResponse response = friendshipService.sendFriendRequest(userEmail, friendshipRequest.getReceiverId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error sending friend request: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Chấp nhận lời mời kết bạn
     */
    @PostMapping("/accept/{requestId}")
    public ResponseEntity<ApiResponse<FriendshipResponse>> acceptFriendRequest(
            HttpServletRequest request,
            @PathVariable Integer requestId) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            FriendshipResponse response = friendshipService.respondToFriendRequest(userEmail, requestId, true);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error accepting friend request: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Từ chối lời mời kết bạn
     */
    @PostMapping("/reject/{requestId}")
    public ResponseEntity<ApiResponse<FriendshipResponse>> rejectFriendRequest(
            HttpServletRequest request,
            @PathVariable Integer requestId) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            FriendshipResponse response = friendshipService.respondToFriendRequest(userEmail, requestId, false);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error rejecting friend request: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Hủy kết bạn
     */
    @DeleteMapping("/{friendId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unfriend(
            HttpServletRequest request,
            @PathVariable Integer friendId) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            friendshipService.unfriend(userEmail, friendId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã hủy kết bạn thành công");
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error unfriending: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Chặn người dùng
     */
    @PostMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> blockUser(
            HttpServletRequest request,
            @PathVariable Integer userId) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            friendshipService.blockUser(userEmail, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã chặn người dùng thành công");
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error blocking user: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Lấy danh sách lời mời kết bạn
     */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getFriendRequests(
            HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            List<FriendshipResponse> pendingRequests = friendshipService.getFriendRequests(userEmail);
            return ResponseEntity.ok(ApiResponse.success(pendingRequests));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting friend requests: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Lấy danh sách bạn bè
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendDto>>> getFriends(
            HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            List<FriendDto> friends = friendshipService.getFriendsList(userEmail);
            return ResponseEntity.ok(ApiResponse.success(friends));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting friends: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Kiểm tra trạng thái bạn bè
     */
    @GetMapping("/check/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkFriendshipStatus(
            HttpServletRequest request,
            @PathVariable Integer userId) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            boolean areFriends = friendshipService.areFriends(userEmail, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("areFriends", areFriends);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking friendship status: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Tìm kiếm người dùng theo email hoặc tên
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> searchUsers(
            HttpServletRequest request,
            @RequestParam String keyword) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            List<UserSearchResponse> searchResults = friendshipService.searchUsers(userEmail, keyword);
            return ResponseEntity.ok(ApiResponse.success(searchResults));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
} 