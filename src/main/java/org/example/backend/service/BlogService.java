package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.BlogPostRequest;
import org.example.backend.entity.BlogPost;
import org.example.backend.entity.TravelPlan;
import org.example.backend.entity.User;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.repository.BlogPostRepository;
import org.example.backend.repository.TravelPlanRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service

@RequiredArgsConstructor
public class BlogService {
    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
    @Transactional
    public BlogPost createBlogPost(String userEmail, BlogPostRequest request) {
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
        BlogPost blogPost = new BlogPost();
        blogPost.setTitle(request.getTitle());
        blogPost.setContent(request.getContent());
        blogPost.setUser(user);
        blogPost.setStatus(BlogPost.PostStatus.PUBLISHED);
        if(travelPlan != null) {
            blogPost.setTravelPlan(travelPlan);
        }
        return blogPostRepository.save(blogPost);
    }
    public List<BlogPost> getAllBlogPosts() {
        return blogPostRepository.findAllByOrderByCreatedAtDesc();
    }
    public Optional<BlogPost> getBlogPostById(Integer id) {
        return blogPostRepository.findById(id);
    }

    public List<BlogPost> getUserPost(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
            return blogPostRepository.findByUserOrderByCreatedAtDesc(user);
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
            // Bỏ qua việc kiểm tra user tồn tại tạm thời
            /*User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));*/
                    
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
        } catch (Exception e) {
            e.printStackTrace();
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    @Transactional
    public BlogPost saveBlogPost(BlogPost blogPost) {
        return blogPostRepository.save(blogPost);
    }
}

