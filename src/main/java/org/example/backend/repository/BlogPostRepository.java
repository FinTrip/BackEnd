package org.example.backend.repository;

import org.example.backend.entity.BlogPost;
import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Integer> {
    List<BlogPost> findAllByOrderByCreatedAtDesc();
    List<BlogPost> findByUserOrderByCreatedAtDesc(User user);
    
    // Đếm số lượng bài viết của người dùng
    long countByUser(User user);
}
