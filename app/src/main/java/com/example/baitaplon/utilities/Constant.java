package com.example.baitaplon.utilities;

import java.util.HashMap;

public class Constant {
    public static final String KEY_COLLECTION_USERS = "users";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_PREFERENCE_NAME = "chatAppPreference";
    public static final String KEY_IS_SIGN_IN = "isSignedIn";
    public static final String KEY_USER_ID = "userID";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_FILE_URL = "fileURL";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_USER = "user";
    public static final String KEY_COLLECTION_CHAT = "chat";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_RECEIVER_ID = "receiverId";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_COLLECTION_CONVERSATIONS = "conversations";
    public static final String KEY_SENDER_NAME = "senderName";
    public static final String KEY_RECEIVER_NAME = "receiverName";
    public static final String KEY_SENDER_IMAGE = "senderImage" ;
    public static final String KEY_RECEIVER_IMAGE = "receiverImage";
    public static final String KEY_SENDER_FILE = "senderFile" ;
    public static final String KEY_RECEIVER_File = "receiverFile";
    public static final String KEY_LAST_MESSAGE = "lastMessage";
    public static final String KEY_AVAILABILITY = "availability";
    public static final String REMOTE_MSG_AUTHORIZATION = "Authorization";
    public static final String REMOTE_MSG_CONTENT_TYPE = "Content-Type";
    public static final String REMOTE_MSG_DATA = "data";
    public static final String REMOTE_MSG_REGISTRATION_IDS = "registration_ids";
    public static final int REQUEST_CODE_PICK_IMAGE = 1;
    public static final int PICK_FILE_REQUEST_CODE = 2;
    public static HashMap<String, String> remoteMsgHeaders = null;
    public static HashMap<String, String> getRemoteMsgHeaders() {
        if(remoteMsgHeaders == null) {
            remoteMsgHeaders = new HashMap<>();
            remoteMsgHeaders.put(REMOTE_MSG_AUTHORIZATION, "key=AAAARtcjThk:APA91bH4ov1pPNukibFFLAEz-u2WoUWUEJRIh07cIvs8Gf11VSxB8ldMmkPiGEBAW9zcILRpOVZfwGN-Y7Xwr22Yz256MVYLdXt0m5CUQ07OH37valeBgKvJuo89i2VIvpIw9LJ5IDeQ");
            remoteMsgHeaders.put(
                    REMOTE_MSG_CONTENT_TYPE,
                    "application/json"
            );
        }
        return remoteMsgHeaders;
    }
}
