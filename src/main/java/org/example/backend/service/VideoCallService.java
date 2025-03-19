package org.example.backend.service;

import org.springframework.stereotype.Service;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.json.simple.JSONObject;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class VideoCallService {

    private final long appID = 1840853797;
    private final String serverSecret = "f1ace14a8bfe8accccc86ffb62f46448";

    private String generateKitTokenForTest(String roomID, String userID, String userEmail) {
        return String.format("%d_%s_%s_%s_%s", 
            appID, 
            serverSecret, 
            roomID, 
            userID, 
            userEmail
        );
    }

    public Map<String, String> createRoom(String userEmail) {
        String roomID = String.valueOf(new Random().nextInt(10000));
        String userID = String.valueOf(new Random().nextInt(10000));
        String kitToken = generateKitTokenForTest(roomID, userID, userEmail);

        Map<String, String> response = new HashMap<>();
        response.put("roomID", roomID);
        response.put("kitToken", kitToken);
        response.put("userID", userID);
        response.put("roomLink", "http://127.0.0.1:5500/VideoCall/src/main/resources/static/js/videocall.html?roomID=" + roomID + "&username=" + userEmail);
        return response;
    }

    public Map<String, String> joinRoom(String roomID, String userEmail) {
        String userID = String.valueOf(new Random().nextInt(10000));
        String kitToken = generateKitTokenForTest(roomID, userID, userEmail);

        Map<String, String> response = new HashMap<>();
        response.put("roomID", roomID);
        response.put("kitToken", kitToken);
        response.put("userID", userID);
        response.put("roomLink", "http://127.0.0.1:5500/VideoCall/src/main/resources/static/js/videocall.html?roomID=" + roomID + "&username=" + userEmail);
        return response;
    }

    public String endRoom(String roomID) {
        return "Room " + roomID + " has been ended.";
    }
}