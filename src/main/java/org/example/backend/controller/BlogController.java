package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.ApiResponse;
import org.example.backend.dto.BlogPostRequest;
import org.example.backend.dto.CommentRequest;
import org.example.backend.entity.BlogPost;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.service.BlogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {
    private final BlogService blogService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPost(
            HttpServletRequest request,
            @Valid @RequestBody BlogPostRequest blogPostRequest) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            BlogPost post = blogService.createBlogPost(userEmail, blogPostRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", post.getId());
            response.put("title", post.getTitle());
            response.put("content", post.getContent());
            response.put("authorName", post.getUser().getFullName());
            response.put("createdAt", post.getCreatedAt());
            
            if (post.getTravelPlan() != null) {
                Map<String, Object> travelPlanInfo = new HashMap<>();
                travelPlanInfo.put("id", post.getTravelPlan().getId());
                travelPlanInfo.put("startDate", post.getTravelPlan().getStartDate());
                travelPlanInfo.put("endDate", post.getTravelPlan().getEndDate());
                response.put("travelPlan", travelPlanInfo);
            }

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating blog post", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllPosts() {
        try {
            List<BlogPost> posts = blogService.getAllBlogPosts();
            List<Map<String, Object>> response = posts.stream()
                .map(post -> {
                    Map<String, Object> postMap = new HashMap<>();
                    postMap.put("id", post.getId());
                    postMap.put("title", post.getTitle());
                    postMap.put("content", post.getContent());
                    postMap.put("authorName", post.getUser().getFullName());
                    postMap.put("createdAt", post.getCreatedAt());
                    return postMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting all posts", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/post/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPostById(@PathVariable Integer id) {
        try {
            BlogPost posts = blogService.getBlogPostById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
            
            // Increment view count when post is viewed
            blogService.increaseViewCount(id);
            
            Map<String, Object> postMap = new HashMap<>();
            postMap.put("id", posts.getId());
            postMap.put("title", posts.getTitle());
            postMap.put("content", posts.getContent());
            postMap.put("authorName", posts.getUser().getFullName());
            postMap.put("createdAt", posts.getCreatedAt());
            postMap.put("views", posts.getViews());
            postMap.put("likes", posts.getLikes());

            return ResponseEntity.ok(ApiResponse.success(postMap));
        } catch (AppException e) {
            log.error("Error getting user posts", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserPosts(
            HttpServletRequest request,
            @Valid @RequestBody BlogPostRequest blogPostRequest
            ) {
        try {
            List<BlogPost> posts = blogService.getUserPost("userEmail");
            List<Map<String, Object>> response = posts.stream()
                .map(post -> {
                    Map<String, Object> postMap = new HashMap<>();
                    postMap.put("id", post.getId());
                    postMap.put("title", post.getTitle());
                    postMap.put("content", post.getContent());
                    postMap.put("createdAt", post.getCreatedAt());
                    if (post.getTravelPlan() != null) {
                        postMap.put("travelPlanId", post.getTravelPlan().getId());
                    }
                    return postMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting user posts", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> likePost(
            @PathVariable Integer postId,
            HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }
            
            boolean liked = blogService.likePost(postId, userEmail);
            Map<String, Object> response = new HashMap<>();
            response.put("liked", liked);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error liking post: {}", e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/hot-trend")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHotTrendPosts() {
        try {
            List<BlogPost> hotPosts = blogService.getHotTrendPosts();
            List<Map<String, Object>> response = hotPosts.stream()
                .map(post -> {
                    Map<String, Object> postMap = new HashMap<>();
                    postMap.put("id", post.getId());
                    postMap.put("title", post.getTitle());
                    postMap.put("content", post.getContent());
                    postMap.put("authorName", post.getUser().getFullName());
                    postMap.put("createdAt", post.getCreatedAt());
                    postMap.put("views", post.getViews());
                    postMap.put("likes", post.getLikes());
                    postMap.put("commentsCount", post.getCommentsCount());
                    postMap.put("hotScore", calculateHotTrendScore(post));
                    
                    return postMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting hot trend posts", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private int calculateHotTrendScore(BlogPost post) {
        int views = post.getViews() != null ? post.getViews() : 0;
        int likes = post.getLikes() != null ? post.getLikes() : 0;
        int comments = post.getCommentsCount() != null ? post.getCommentsCount() : 0;
        
        return views + (likes * 3) + (comments * 5);
    }
}
