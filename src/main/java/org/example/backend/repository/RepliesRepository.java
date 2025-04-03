package org.example.backend.repository;

import org.example.backend.entity.Comments;
import org.example.backend.entity.Replies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepliesRepository extends JpaRepository<Replies, Long> {
    List<Replies> findByCommentsOrderByCreatedAtDesc(Comments comment);
}
