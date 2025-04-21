package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.FriendDto;
import org.example.backend.dto.FriendshipResponse;
import org.example.backend.dto.UserSearchResponse;
import org.example.backend.entity.Friendship;
import org.example.backend.entity.User;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.repository.FriendshipRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    @Transactional
    public FriendshipResponse sendFriendRequest(String senderEmail, Integer receiverId) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Không thể gửi lời mời kết bạn cho chính mình
        if (sender.getId().equals(receiver.getId())) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }
        
        // Kiểm tra xem lời mời kết bạn đã tồn tại chưa
        Optional<Friendship> existingFriendship = friendshipRepository.findBySenderAndReceiver(sender, receiver);
        if (existingFriendship.isPresent()) {
            Friendship.FriendshipStatus status = existingFriendship.get().getStatus();
            if (status == Friendship.FriendshipStatus.PENDING) {
                throw new AppException(ErrorCode.FRIEND_REQUEST_ALREADY_SENT);
            } else if (status == Friendship.FriendshipStatus.ACCEPTED) {
                throw new AppException(ErrorCode.ALREADY_FRIENDS);
            } else if (status == Friendship.FriendshipStatus.BLOCKED) {
                throw new AppException(ErrorCode.USER_BLOCKED);
            }
            
            // Nếu đã bị từ chối thì có thể gửi lại
            Friendship friendship = existingFriendship.get();
            friendship.setStatus(Friendship.FriendshipStatus.PENDING);
            return FriendshipResponse.fromEntity(friendshipRepository.save(friendship));
        }
        
        // Kiểm tra xem đã có lời mời từ người nhận chưa
        Optional<Friendship> reverseRequest = friendshipRepository.findBySenderAndReceiver(receiver, sender);
        if (reverseRequest.isPresent()) {
            Friendship.FriendshipStatus status = reverseRequest.get().getStatus();
            if (status == Friendship.FriendshipStatus.PENDING) {
                // Tự động chấp nhận lời mời nếu người nhận đã gửi lời mời cho mình trước đó
                Friendship friendship = reverseRequest.get();
                friendship.setStatus(Friendship.FriendshipStatus.ACCEPTED);
                return FriendshipResponse.fromEntity(friendshipRepository.save(friendship));
            } else if (status == Friendship.FriendshipStatus.ACCEPTED) {
                throw new AppException(ErrorCode.ALREADY_FRIENDS);
            } else if (status == Friendship.FriendshipStatus.BLOCKED) {
                throw new AppException(ErrorCode.USER_BLOCKED);
            }
        }
        
        // Tạo lời mời kết bạn mới
        Friendship newFriendship = new Friendship();
        newFriendship.setSender(sender);
        newFriendship.setReceiver(receiver);
        newFriendship.setStatus(Friendship.FriendshipStatus.PENDING);
        
        return FriendshipResponse.fromEntity(friendshipRepository.save(newFriendship));
    }
    
    @Transactional
    public FriendshipResponse respondToFriendRequest(String receiverEmail, Integer requestId, boolean accept) {
        User receiver = userRepository.findByEmail(receiverEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
        
        // Kiểm tra xem người dùng có phải là người nhận lời mời không
        if (!friendship.getReceiver().getId().equals(receiver.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        
        // Kiểm tra xem lời mời có đang ở trạng thái chờ không
        if (friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }
        
        // Cập nhật trạng thái
        friendship.setStatus(accept ? Friendship.FriendshipStatus.ACCEPTED : Friendship.FriendshipStatus.REJECTED);
        
        return FriendshipResponse.fromEntity(friendshipRepository.save(friendship));
    }
    
    @Transactional
    public void unfriend(String userEmail, Integer friendId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Tìm kiếm quan hệ bạn bè theo cả hai chiều
        Optional<Friendship> friendship1 = friendshipRepository.findBySenderAndReceiver(user, friend);
        Optional<Friendship> friendship2 = friendshipRepository.findBySenderAndReceiver(friend, user);
        
        boolean isFriend = (friendship1.isPresent() && friendship1.get().getStatus() == Friendship.FriendshipStatus.ACCEPTED) ||
                (friendship2.isPresent() && friendship2.get().getStatus() == Friendship.FriendshipStatus.ACCEPTED);
        
        if (!isFriend) {
            throw new AppException(ErrorCode.NOT_FRIENDS);
        }
        
        // Xóa quan hệ bạn bè
        if (friendship1.isPresent()) {
            friendshipRepository.delete(friendship1.get());
        }
        
        if (friendship2.isPresent()) {
            friendshipRepository.delete(friendship2.get());
        }
    }
    
    @Transactional
    public void blockUser(String userEmail, Integer blockUserId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        User blockUser = userRepository.findById(blockUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Xóa mọi quan hệ bạn bè hiện có
        Optional<Friendship> existingFriendship1 = friendshipRepository.findBySenderAndReceiver(user, blockUser);
        Optional<Friendship> existingFriendship2 = friendshipRepository.findBySenderAndReceiver(blockUser, user);
        
        if (existingFriendship1.isPresent()) {
            friendshipRepository.delete(existingFriendship1.get());
        }
        
        if (existingFriendship2.isPresent()) {
            friendshipRepository.delete(existingFriendship2.get());
        }
        
        // Tạo quan hệ chặn mới
        Friendship blockFriendship = new Friendship();
        blockFriendship.setSender(user);
        blockFriendship.setReceiver(blockUser);
        blockFriendship.setStatus(Friendship.FriendshipStatus.BLOCKED);
        
        friendshipRepository.save(blockFriendship);
    }
    
    public List<FriendshipResponse> getFriendRequests(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        List<Friendship> pendingRequests = friendshipRepository.findByReceiverAndStatus(user, Friendship.FriendshipStatus.PENDING);
        
        return pendingRequests.stream()
                .map(FriendshipResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<FriendshipResponse> getFriends(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Chỉ lấy các mối quan hệ có trạng thái ACCEPTED
        List<Friendship> sentAccepted = friendshipRepository.findBySenderAndStatus(user, Friendship.FriendshipStatus.ACCEPTED);
        List<Friendship> receivedAccepted = friendshipRepository.findByReceiverAndStatus(user, Friendship.FriendshipStatus.ACCEPTED);
        
        List<FriendshipResponse> result = new ArrayList<>();
        
        // Chuyển đổi mối quan hệ thành response và thêm vào kết quả
        result.addAll(sentAccepted.stream()
                .map(FriendshipResponse::fromEntity)
                .collect(Collectors.toList()));
        
        result.addAll(receivedAccepted.stream()
                .map(FriendshipResponse::fromEntity)
                .collect(Collectors.toList()));
        
        return result;
    }
    
    /**
     * Lấy danh sách bạn bè của người dùng với thông tin tối giản hơn
     */
    public List<FriendDto> getFriendsList(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Lấy tất cả mối quan hệ bạn bè đã được chấp nhận
        List<Friendship> sentAccepted = friendshipRepository.findBySenderAndStatus(user, Friendship.FriendshipStatus.ACCEPTED);
        List<Friendship> receivedAccepted = friendshipRepository.findByReceiverAndStatus(user, Friendship.FriendshipStatus.ACCEPTED);
        
        List<FriendDto> result = new ArrayList<>();
        
        // Chuyển đổi mỗi mối quan hệ thành đối tượng FriendDto
        result.addAll(sentAccepted.stream()
                .map(friendship -> FriendDto.fromFriendship(friendship, userEmail))
                .collect(Collectors.toList()));
        
        result.addAll(receivedAccepted.stream()
                .map(friendship -> FriendDto.fromFriendship(friendship, userEmail))
                .collect(Collectors.toList()));
        
        return result;
    }
    
    public boolean areFriends(String userEmail, Integer otherUserId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        return friendshipRepository.existsBySenderAndReceiverAndStatus(user, otherUser, Friendship.FriendshipStatus.ACCEPTED) ||
                friendshipRepository.existsByReceiverAndSenderAndStatus(user, otherUser, Friendship.FriendshipStatus.ACCEPTED);
    }
    
    /**
     * Tìm kiếm người dùng theo email hoặc tên đầy đủ
     * @param userEmail email của người dùng hiện tại
     * @param keyword từ khóa tìm kiếm (email hoặc fullName)
     * @return danh sách người dùng phù hợp
     */
    public List<UserSearchResponse> searchUsers(String userEmail, String keyword) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        List<User> foundUsers = userRepository.searchByEmailOrFullName(keyword);
        
        // Loại bỏ người dùng hiện tại khỏi kết quả tìm kiếm
        foundUsers = foundUsers.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .collect(Collectors.toList());
        
        return foundUsers.stream().map(user -> {
            // Kiểm tra xem đã là bạn bè chưa
            boolean alreadyFriend = areFriends(userEmail, user.getId());
            
            // Kiểm tra xem đã gửi lời mời kết bạn chưa
            boolean requestPending = isPendingFriendRequest(currentUser, user);
            
            return UserSearchResponse.fromUser(user, alreadyFriend, requestPending);
        }).collect(Collectors.toList());
    }
    
    /**
     * Kiểm tra xem có lời mời kết bạn đang chờ giữa hai người dùng không
     */
    private boolean isPendingFriendRequest(User sender, User receiver) {
        Optional<Friendship> request1 = friendshipRepository.findBySenderAndReceiver(sender, receiver);
        Optional<Friendship> request2 = friendshipRepository.findBySenderAndReceiver(receiver, sender);
        
        return (request1.isPresent() && request1.get().getStatus() == Friendship.FriendshipStatus.PENDING) ||
               (request2.isPresent() && request2.get().getStatus() == Friendship.FriendshipStatus.PENDING);
    }
} 