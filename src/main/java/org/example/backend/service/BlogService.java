package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.BlogPostRequest;
import org.example.backend.entity.BlogPost;
import org.example.backend.entity.Comments;
import org.example.backend.entity.TravelPlan;
import org.example.backend.entity.User;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.repository.BlogPostRepository;
import org.example.backend.repository.CommentRepository;
import org.example.backend.repository.TravelPlanRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogService {
    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final UploadImageFile uploadImageFile;
    private final CommentRepository commentRepository;

    @Transactional
    public BlogPost createBlogPost(String userEmail, BlogPostRequest request, List<MultipartFile> images) {
        //Tìm user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(()-> new AppException(ErrorCode.USER_NOT_FOUND));

        //Tim travel plan
        TravelPlan travelPlan = null;
        if(request.getTravelPlanId() != null) {
            travelPlan = travelPlanRepository.findById(request.getTravelPlanId())
                    .orElseThrow(()->new AppException(ErrorCode.TRAVELPLAN_NOT_FOUND));
            //Kiem tra Plan co thuoc User hay khong
            if(!travelPlan.getUser().getId().equals(user.getId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
            }
        }

        // Upload images to Cloudinary and get URLs
        List<String> imageUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                try {
                    if (!image.isEmpty()) {
                        String imageUrl = uploadImageFile.uploadImage(image);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            imageUrls.add(imageUrl);
                        }
                    }
                } catch (IOException e) {
                    log.error("Error uploading image: ", e);
                    throw new AppException(ErrorCode.IMAGE_UPLOAD_ERROR);
                }
            }
        }

        if (imageUrls.isEmpty()) {
            throw new AppException(ErrorCode.FILE_UPLOAD_ERROR, "Phải upload ít nhất 1 ảnh cho bài viết");
        }

        BlogPost blogPost = new BlogPost();
        blogPost.setTitle(request.getTitle());
        blogPost.setContent(request.getContent());
        blogPost.setImages(String.join(",", imageUrls)); // Store image URLs as comma-separated string
        blogPost.setUser(user);
        blogPost.setStatus(BlogPost.PostStatus.PUBLISHED);
        blogPost.setViews(0);
        blogPost.setLikes(0);
        blogPost.setCommentsCount(0);
        
        if(travelPlan != null) {
            blogPost.setTravelPlan(travelPlan);
        }
        
        return blogPostRepository.save(blogPost);
    }
    public List<BlogPost> getAllBlogPosts() {
        return blogPostRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(post -> post.getStatus() != BlogPost.PostStatus.ARCHIVED)
                .collect(Collectors.toList());
    }
    public Optional<BlogPost> getBlogPostById(Integer id) {
        Optional<BlogPost> post = blogPostRepository.findById(id);
        
        if (post.isPresent() && post.get().getStatus() == BlogPost.PostStatus.ARCHIVED) {
            // Return empty if post is archived
            return Optional.empty();
        }
        
        return post;
    }

    public List<BlogPost> getUserPost(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        return blogPostRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(post -> post.getStatus() != BlogPost.PostStatus.ARCHIVED)
                .collect(Collectors.toList());
    }
    
    public List<BlogPost> getPostsByUserId(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User with ID " + userId + " not found"));
        
        log.info("Fetching posts for user ID: {}", userId);
        return blogPostRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(post -> post.getStatus() != BlogPost.PostStatus.ARCHIVED)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void increaseViewCount(Integer postId) {
        try {
            BlogPost blogPost = blogPostRepository.findById(postId)
                    .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
                    
            // Kiểm tra và khởi tạo views nếu null
            if (blogPost.getViews() == null) {
                blogPost.setViews(0);
            }
            
            blogPost.setViews(blogPost.getViews() + 1);
            blogPostRepository.save(blogPost);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    @Transactional
    public boolean likePost(Integer postId, String userEmail) {
        try {
            // Kiểm tra user tồn tại
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                    
            BlogPost blogPost = blogPostRepository.findById(postId)
                    .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

            // Khởi tạo giá trị mặc định nếu null
            if (blogPost.getLikes() == null) {
                blogPost.setLikes(0);
            }
            if (blogPost.getLikedUserEmails() == null) {
                blogPost.setLikedUserEmails("");
            }
            
            List<String> likedEmails = new ArrayList<>();
            if (!blogPost.getLikedUserEmails().isEmpty()) {
                likedEmails = new ArrayList<>(List.of(blogPost.getLikedUserEmails().split(",")));
            }

            boolean isLiked;
            if (likedEmails.contains(userEmail)) {
                // Unlike
                likedEmails.remove(userEmail);
                blogPost.setLikes(Math.max(0, blogPost.getLikes() - 1));
                isLiked = false;
            } else {
                // Like
                likedEmails.add(userEmail);
                blogPost.setLikes(blogPost.getLikes() + 1);
                isLiked = true;
            }
            
            blogPost.setLikedUserEmails(likedEmails.isEmpty() ? "" : String.join(",", likedEmails));
            blogPostRepository.save(blogPost);
            
            return isLiked;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    @Transactional
    public BlogPost saveBlogPost(BlogPost blogPost) {
        return blogPostRepository.save(blogPost);
    }

    public List<BlogPost> getHotTrendPosts() {
        try {
            List<BlogPost> allPosts = blogPostRepository.findAll().stream()
                    .filter(post -> post.getStatus() != BlogPost.PostStatus.ARCHIVED)
                    .collect(Collectors.toList());
            
            // Sắp xếp bài viết theo điểm hot trend
            return allPosts.stream()
                .sorted((post1, post2) -> {
                    int score1 = calculateHotTrendScore(post1);
                    int score2 = calculateHotTrendScore(post2);
                    return score2 - score1; // Sắp xếp giảm dần
                })
                .limit(10) // Lấy 10 bài viết hot nhất
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private int calculateHotTrendScore(BlogPost post) {
        int views = post.getViews() != null ? post.getViews() : 0;
        int likes = post.getLikes() != null ? post.getLikes() : 0;
        int comments = post.getCommentsCount() != null ? post.getCommentsCount() : 0;
        
        return views + (likes * 3) + (comments * 5);
    }

    @Transactional
    public BlogPost updateBlogPost(String userEmail, Integer postId, BlogPostRequest request, List<MultipartFile> newImages) {
        log.info("Updating blog post with ID: {}", postId);
        
        // Tìm user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                
        // Tìm bài viết
        BlogPost blogPost = blogPostRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
                
        // Kiểm tra quyền sở hữu
        if (!blogPost.getUser().getId().equals(user.getId())) {
            log.warn("User {} attempted to update post owned by user {}", user.getId(), blogPost.getUser().getId());
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS, "You can only update your own posts");
        }
        
        log.info("Updating post - current details: title='{}', content='{}'", blogPost.getTitle(), blogPost.getContent());
        
        // Cập nhật tiêu đề và nội dung
        blogPost.setTitle(request.getTitle());
        blogPost.setContent(request.getContent());
        
        // Cập nhật travel plan nếu có
        if (request.getTravelPlanId() != null) {
            TravelPlan travelPlan = travelPlanRepository.findById(request.getTravelPlanId())
                    .orElseThrow(() -> new AppException(ErrorCode.TRAVELPLAN_NOT_FOUND));
                    
            // Kiểm tra plan có thuộc User hay không
            if (!travelPlan.getUser().getId().equals(user.getId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
            }
            
            blogPost.setTravelPlan(travelPlan);
            log.info("Updated travel plan to ID: {}", travelPlan.getId());
        }
        
        // Xử lý ảnh
        List<String> finalImageUrls = new ArrayList<>();
        
        // 1. Xử lý trường hợp có danh sách ảnh từ request (đã xử lý ở frontend)
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // Sử dụng CHÍNH XÁC danh sách ảnh từ request, THAY THẾ hoàn toàn danh sách cũ
            finalImageUrls = new ArrayList<>(Arrays.asList(request.getImages().split(",")));
            log.info("Using EXACTLY the images from request: {}", finalImageUrls);
        } else {
            // 2. Nếu không có danh sách ảnh từ request, giữ nguyên ảnh cũ
            if (blogPost.getImages() != null && !blogPost.getImages().isEmpty()) {
                finalImageUrls = new ArrayList<>(Arrays.asList(blogPost.getImages().split(",")));
                log.info("No image info in request, keeping existing: {}", finalImageUrls);
            }
            
            // 3. Chỉ thêm ảnh mới nếu không có request.images (trường hợp upload ảnh mới mà không gửi danh sách cũ)
            if (newImages != null && !newImages.isEmpty()) {
                for (MultipartFile image : newImages) {
                    try {
                        if (!image.isEmpty()) {
                            String imageUrl = uploadImageFile.uploadImage(image);
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                finalImageUrls.add(imageUrl);
                                log.info("Added new uploaded image: {}", imageUrl);
                            }
                        }
                    } catch (IOException e) {
                        log.error("Error uploading image: ", e);
                        throw new AppException(ErrorCode.IMAGE_UPLOAD_ERROR);
                    }

                }
            }
        }
        
        // Lưu danh sách ảnh cuối cùng
        blogPost.setImages(finalImageUrls.isEmpty() ? null : String.join(",", finalImageUrls));
        log.info("Final images after update: {}", blogPost.getImages());
        
        BlogPost savedPost = blogPostRepository.save(blogPost);
        log.info("Successfully updated blog post with ID: {}", savedPost.getId());
        return savedPost;
    }
    
    @Transactional
    public void deleteBlogPost(String userEmail, Integer postId) {
        log.info("Attempting to delete blog post with ID: {}", postId);
        
        // Tìm user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                
        // Tìm bài viết
        BlogPost blogPost = blogPostRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
                
        // Kiểm tra quyền sở hữu
        if (!blogPost.getUser().getId().equals(user.getId())) {
            log.warn("User {} attempted to delete post owned by user {}", user.getId(), blogPost.getUser().getId());
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS, "You can only delete your own posts");
        }
        
        try {
            // QUAN TRỌNG: Xóa tất cả comment của bài viết trước
            List<Comments> comments = commentRepository.findByPostId(postId);
            if (!comments.isEmpty()) {
                log.info("Deleting {} comments for blog post ID: {}", comments.size(), postId);
                commentRepository.deleteAll(comments);
            }
            
            // Sau đó xóa bài viết
            blogPostRepository.delete(blogPost);
            log.info("Successfully deleted blog post with ID: {}", postId);
        } catch (Exception e) {
            log.error("Error deleting blog post: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error deleting blog post: " + e.getMessage());
        }
    }
}

