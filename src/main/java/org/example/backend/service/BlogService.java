package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;

@Service
@Slf4j
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

    public List<BlogPost> getUserPost(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
            return blogPostRepository.findByUserOrderByCreatedAtDesc(user);
    }
}

