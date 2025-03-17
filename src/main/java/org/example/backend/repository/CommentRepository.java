package org.example.backend.repository;

import org.example.backend.entity.Comments;
import org.example.backend.entity.BlogPost;
import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comments, Long> {
    //Tìm tất cả các comment của một post, sắp xếp theo thời gian giảm dần
    List<Comments> findByPostOrderByCreatedAtDesc(BlogPost post);
    //Tìm tất cả các comment của một user, sắp xếp theo thời gian giảm dần
    List<Comments> findByUserOrderByCreatedAtDesc(User user);
    List<Comments> findByPostId(int postId);

}
