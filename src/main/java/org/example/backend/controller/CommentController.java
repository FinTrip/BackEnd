package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.ApiResponse;
import org.example.backend.dto.CommentRequest;
import org.example.backend.dto.ReplyRequest;
import org.example.backend.entity.Comments;
import org.example.backend.entity.Replies;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.service.CommentService;
import org.example.backend.config.SecurityConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createComment(
            HttpServletRequest request,
            @Valid @RequestBody CommentRequest commentRequest) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }

            Comments comment = commentService.createComment(userEmail, commentRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", comment.getId());
            response.put("content", comment.getContent());
            response.put("authorName", comment.getUser().getFullName());
            response.put("createdAt", comment.getCreatedAt());
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating comment", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/reply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReply(
            HttpServletRequest request,
            @Valid @RequestBody ReplyRequest replyRequest) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }

            Replies reply = commentService.createReply(userEmail, replyRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", reply.getId());
            response.put("content", reply.getContent());
            response.put("authorName", reply.getUser().getFullName());
            response.put("createdAt", reply.getCreatedAt());
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating reply", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPostComments(
            @PathVariable int postId) {
        try {
            List<Comments> comments = commentService.getPostComments(postId);
            List<Map<String, Object>> response = comments.stream()
                .map(comment -> {
                    Map<String, Object> commentMap = new HashMap<>();
                    commentMap.put("id", comment.getId());
                    commentMap.put("content", comment.getContent());
                    commentMap.put("authorName", comment.getUser().getFullName());
                    commentMap.put("createdAt", comment.getCreatedAt());
                    return commentMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting comments for post: {}", postId, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCommentReplies(
            @PathVariable Integer commentId) {
        try {
            List<Replies> replies = commentService.getCommentReplies(commentId);
            List<Map<String, Object>> response = replies.stream()
                .map(reply -> {
                    Map<String, Object> replyMap = new HashMap<>();
                    replyMap.put("id", reply.getId());
                    replyMap.put("content", reply.getContent());
                    replyMap.put("authorName", reply.getUser().getFullName());
                    replyMap.put("createdAt", reply.getCreatedAt());
                    return replyMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting replies for comment: {}", commentId, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<String>> deleteComment(
            @PathVariable Integer commentId) {
        try {
            String userEmail = SecurityConfig.getCurrentUserEmail();
            log.info("Deleting comment {} for user: {}", commentId, userEmail);
            
            commentService.deleteComment(userEmail, commentId);
            return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully"));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting comment", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/reply/{replyId}")
    public ResponseEntity<ApiResponse<String>> deleteReply(
            @PathVariable Integer replyId) {
        try {
            String userEmail = SecurityConfig.getCurrentUserEmail();
            log.info("Deleting reply {} for user: {}", replyId, userEmail);
            
            commentService.deleteReply(userEmail, replyId);
            return ResponseEntity.ok(ApiResponse.success("Reply deleted successfully"));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting reply", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
