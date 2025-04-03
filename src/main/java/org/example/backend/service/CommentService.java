package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.CommentRequest;
import org.example.backend.dto.ReplyRequest;
import org.example.backend.entity.Comments;
import org.example.backend.entity.BlogPost;
import org.example.backend.entity.User;
import org.example.backend.entity.Replies;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.repository.CommentRepository;
import org.example.backend.repository.BlogPostRepository;
import org.example.backend.repository.RepliesRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final BlogPostRepository postRepository;
    private final UserRepository userRepository;
    private final RepliesRepository replyRepository;

    public Comments createComment(String userEmail, CommentRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Comment content cannot be empty");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        BlogPost post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        Comments comment = Comments.builder()
                .content(request.getContent().trim())
                .post(post)
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return commentRepository.save(comment);
    }

    public Replies createReply(String userEmail, ReplyRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Reply content cannot be empty");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Comments comment = commentRepository.findById(request.getCommentId().longValue())
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        Replies reply = new Replies();
        reply.setContent(request.getContent().trim());
        reply.setComments(comment);
        reply.setUser(user);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());

        return replyRepository.save(reply);
    }

    public List<Comments> getPostComments(Integer postId) {
        BlogPost post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        return commentRepository.findByPostOrderByCreatedAtDesc(post);
    }

    public List<Replies> getCommentReplies(Integer commentId) {
        Comments comment = commentRepository.findById(commentId.longValue())
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        return replyRepository.findByCommentsOrderByCreatedAtDesc(comment);
    }

    public void deleteComment(String userEmail, Integer commentId) {
        Comments comment = commentRepository.findById(commentId.longValue())
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getEmail().equals(userEmail)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }

        commentRepository.delete(comment);
    }

    public void deleteReply(String userEmail, Integer replyId) {
        Replies reply = replyRepository.findById(replyId.longValue())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT, "Reply not found"));

        if (!reply.getUser().getEmail().equals(userEmail)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }

        replyRepository.delete(reply);
    }
}
