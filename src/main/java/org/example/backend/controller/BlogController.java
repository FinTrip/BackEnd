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
import org.example.backend.service.UploadImageFile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {
    private final BlogService blogService;
    private final UploadImageFile uploadImageFile;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPost(
            HttpServletRequest request,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "files") MultipartFile[] files,
            @RequestParam(value = "travelPlanId", required = false) Integer travelPlanId) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }

            // Kiểm tra xem có ảnh được upload không
            if (files == null || files.length == 0) {
                throw new AppException(ErrorCode.FILE_UPLOAD_ERROR, "Phải upload ít nhất 1 ảnh cho bài viết");
            }

            // Tạo BlogPostRequest từ các tham số
            BlogPostRequest blogPostRequest = new BlogPostRequest();
            blogPostRequest.setTitle(title);
            blogPostRequest.setContent(content);
            blogPostRequest.setTravelPlanId(travelPlanId);

            // Convert MultipartFile[] to List<MultipartFile> và loại bỏ các file null hoặc empty
            List<MultipartFile> fileList = Arrays.stream(files)
                    .filter(file -> file != null && !file.isEmpty())
                    .collect(Collectors.toList());

            if (fileList.isEmpty()) {
                throw new AppException(ErrorCode.FILE_UPLOAD_ERROR, "Phải upload ít nhất 1 ảnh hợp lệ cho bài viết");
            }

            // Tạo bài viết với danh sách ảnh
            BlogPost post = blogService.createBlogPost(userEmail, blogPostRequest, fileList);
            log.info("Blog post created with ID: {} with {} images", post.getId(), fileList.size());

            List<String> imageUrls = post.getImages() != null && !post.getImages().isEmpty() ?
                    Arrays.asList(post.getImages().split(",")) :
                    new ArrayList<>();

            Map<String, Object> response = new HashMap<>();
            response.put("id", post.getId());
            response.put("title", post.getTitle());
            response.put("content", post.getContent());
            response.put("images", imageUrls);
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
        } catch (Exception e) {
            log.error("Error creating blog post: ", e);
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
                        postMap.put("images", post.getImages() != null ?
                                Arrays.asList(post.getImages().split(",")) :
                                new ArrayList<>());
                        postMap.put("authorName", post.getUser().getFullName());
                        postMap.put("authorId", post.getUser().getId());
                        postMap.put("createdAt", post.getCreatedAt());
                        postMap.put("views", post.getViews());
                        postMap.put("likes", post.getLikes());

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
            BlogPost post = blogService.getBlogPostById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

            // Increment view count when post is viewed
            blogService.increaseViewCount(id);

            Map<String, Object> postMap = new HashMap<>();
            postMap.put("id", post.getId());
            postMap.put("title", post.getTitle());
            postMap.put("content", post.getContent());
            postMap.put("images", post.getImages() != null ?
                    Arrays.asList(post.getImages().split(",")) :
                    new ArrayList<>());
            postMap.put("authorName", post.getUser().getFullName());
            postMap.put("authorId", post.getUser().getId());
            postMap.put("createdAt", post.getCreatedAt());
            postMap.put("views", post.getViews());
            postMap.put("likes", post.getLikes());

            return ResponseEntity.ok(ApiResponse.success(postMap));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting post by id", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserPosts(HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            if (userEmail == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED_USER);
            }

            List<BlogPost> posts = blogService.getUserPost(userEmail);
            List<Map<String, Object>> response = posts.stream()
                    .map(post -> {
                        Map<String, Object> postMap = new HashMap<>();
                        postMap.put("id", post.getId());
                        postMap.put("title", post.getTitle());
                        postMap.put("content", post.getContent());
                        postMap.put("images", post.getImages() != null ?
                                Arrays.asList(post.getImages().split(",")) :
                                new ArrayList<>());
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
