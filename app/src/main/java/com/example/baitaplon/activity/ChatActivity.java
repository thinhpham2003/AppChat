package com.example.baitaplon.activity;

import static android.content.ContentValues.TAG;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.baitaplon.helpers.DatabaseHelper;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import com.example.baitaplon.adapters.ChatAdapter;
import com.example.baitaplon.databinding.ActivityChatBinding;
import com.example.baitaplon.models.Message;
import com.example.baitaplon.models.User;
import com.example.baitaplon.network.ApiClient;
import com.example.baitaplon.network.ApiService;
import com.example.baitaplon.utilities.Constant;
import com.example.baitaplon.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.OnProgressListener;
import com.google.firebase.firestore.QuerySnapshot;

import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private DatabaseHelper databaseHelper;
    private User receiverUser;
    private List<Message> messages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;

    private String conversionId = null;
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
    }


    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        databaseHelper = new DatabaseHelper(this);
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                messages,
                preferenceManager.getString(Constant.KEY_USER_ID),
                getBitmapFromEncodeString(receiverUser.image)

        );
        binding.chatRCV.setAdapter(chatAdapter);
        db = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
        message.put(Constant.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constant.KEY_MESSAGE, binding.inpMsg.getText().toString());
        message.put(Constant.KEY_TIMESTAMP, new Date());
        db.collection(Constant.KEY_COLLECTION_CHAT).add(message);
        if (conversionId != null) {
            updateConversion(binding.inpMsg.getText().toString());
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
            conversion.put(Constant.KEY_SENDER_NAME, preferenceManager.getString(Constant.KEY_NAME));
            conversion.put(Constant.KEY_SENDER_IMAGE, preferenceManager.getString(Constant.KEY_IMAGE));
            conversion.put(Constant.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constant.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constant.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constant.KEY_LAST_MESSAGE, binding.inpMsg.getText().toString());
            conversion.put(Constant.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        if (!isReceiverAvailable) {
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Constant.KEY_USER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
                data.put(Constant.KEY_NAME, preferenceManager.getString(Constant.KEY_NAME));
                data.put(Constant.KEY_FCM_TOKEN, preferenceManager.getString(Constant.KEY_FCM_TOKEN));
                data.put(Constant.KEY_MESSAGE, binding.inpMsg.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Constant.REMOTE_MSG_DATA, data);
                body.put(Constant.REMOTE_MSG_REGISTRATION_IDS, tokens);
                sendNotification(body.toString());
            } catch (Exception e) {
                showToast(e.getMessage());
            }

        }


//        Sqlite
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
        values.put(DatabaseHelper.COLUMN_RECEIVER_ID, receiverUser.id);
        values.put(DatabaseHelper.COLUMN_MESSAGE, binding.inpMsg.getText().toString());
        values.put(DatabaseHelper.COLUMN_DATE_TIME, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        databaseHelper.getWritableDatabase().insert(DatabaseHelper.TABLE_MESSAGES, null, values);
        Log.d(TAG, "sendMessage: " + values);
        Log.d(TAG, "sendMessage: " + databaseHelper.getDatabaseName());
        Log.d(TAG, "sendMessage: " + databaseHelper.getReadableDatabase());
        Log.d(TAG, "sendMessage: " + databaseHelper.getWritableDatabase());
        binding.inpMsg.setText(null);
        loadMessagesFromSQLite();
    }


    private void loadMessagesFromSQLite() {
        Cursor cursor = databaseHelper.getReadableDatabase().query(
                DatabaseHelper.TABLE_MESSAGES,
                new String[] {
                        DatabaseHelper.COLUMN_SENDER_ID,
                        DatabaseHelper.COLUMN_RECEIVER_ID,
                        DatabaseHelper.COLUMN_MESSAGE,
                        DatabaseHelper.COLUMN_DATE_TIME
                },
                DatabaseHelper.COLUMN_SENDER_ID + "=? AND " + DatabaseHelper.COLUMN_RECEIVER_ID + "=?",
                new String[] {preferenceManager.getString(Constant.KEY_USER_ID), receiverUser.id},
                null, null, DatabaseHelper.COLUMN_DATE_TIME + " ASC"
        );
        while (cursor.moveToNext()) {
            // Tạo một đối tượng Message mới và thêm vào danh sách tin nhắn
        }
        cursor.close();
    }



    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messageBody) {
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constant.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                // Log the entire server response
                Log.i(TAG, "Server Response: " + response.body());

                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("result");
                            if (responseJson.getInt("failure") == 1) {
                                JSONObject error = (JSONObject) results.get(0);
                                Log.e(TAG, "onResponse: Fail1 " + error);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "onResponse: Fail2 " + e);
                        e.printStackTrace();
                    }
                    showToast("Notification sent successfully");
                } else {
                    Log.e(TAG, "onResponse: Fail3 " + response.code());
                    showToast("Error: " + response.code());
                }
            }


            @Override
            public void onFailure(Call<String> call, Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void listenAvailabilityOfReceiver() {
        db.collection(Constant.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                if (value.getLong(Constant.KEY_AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(
                            value.getLong(Constant.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable = availability == 1;
                }
                receiverUser.token = value.getString(Constant.KEY_FCM_TOKEN);
                if (receiverUser.image == null) {
                    receiverUser.image = value.getString(Constant.KEY_IMAGE);
                    chatAdapter.setReceiverProfileImage(getBitmapFromEncodeString(receiverUser.image));
                    chatAdapter.notifyItemRangeChanged(0, messages.size());
                }
            }
            if (isReceiverAvailable) {
                binding.txtAvailability.setVisibility(View.VISIBLE);
            } else {
                binding.txtAvailability.setVisibility(View.GONE);
            }
        });
    }

    private void listenMessages() {
        db.collection(Constant.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID))
                .whereEqualTo(Constant.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        db.collection(Constant.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constant.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constant.KEY_RECEIVER_ID, preferenceManager.getString(Constant.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = messages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    Message message = new Message();
                    message.senderId = documentChange.getDocument().getString(Constant.KEY_SENDER_ID);
                    message.receiverId = documentChange.getDocument().getString(Constant.KEY_RECEIVER_ID);
                    message.message = documentChange.getDocument().getString(Constant.KEY_MESSAGE);
                    message.image = documentChange.getDocument().getString(Constant.KEY_IMAGE);
                    message.fileURL = documentChange.getDocument().getString(Constant.KEY_FILE_URL);
                    message.fileName = documentChange.getDocument().getString(Constant.KEY_FILE_NAME);
                    message.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constant.KEY_TIMESTAMP));
                    message.dateObject = documentChange.getDocument().getDate(Constant.KEY_TIMESTAMP);
                    messages.add(message);
                }
            }
            Collections.sort(messages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(messages.size(), messages.size());
                binding.chatRCV.smoothScrollToPosition(messages.size() - 1);
            }
            binding.chatRCV.setVisibility(View.VISIBLE);
        }
        binding.pgrBar.setVisibility(View.GONE);
        if (conversionId == null) {
            checkForConversion();
        }
    };

    private Bitmap getBitmapFromEncodeString(String encodeImage) {
        if (encodeImage != null) {
            byte[] bytes = Base64.decode(encodeImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }

    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constant.KEY_USER);
        binding.txtName.setText(receiverUser.name);
    }

    private void setListeners() {
        binding.imgBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.imgSendIMG.setOnClickListener(v -> sendImage());
        binding.imgSendFile.setOnClickListener(v -> pickFile());
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }


    private void addConversion(HashMap<String, Object> conversion) {
        db.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message) {
        DocumentReference documentReference =
                db.collection(Constant.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Constant.KEY_LAST_MESSAGE, message,
                Constant.KEY_TIMESTAMP, new Date()
        );
    }

    private void checkForConversion() {
        if (messages.size() != 0) {
            checkForConversionRemotely(
                    preferenceManager.getString(Constant.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConversionRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constant.KEY_USER_ID)
            );

        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId) {
        db.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constant.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constant.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
        loadMessagesFromSQLite();
    }


    // Send Image

    private void sendImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, Constant.REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        Bitmap selectedImageBitmap = BitmapFactory.decodeStream(inputStream);
                        if (selectedImageBitmap != null) {
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
                            byte[] imageBytes = byteArrayOutputStream.toByteArray();
                            String imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                            sendMessageWithImage(imageString);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (requestCode == Constant.PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            uploadFile(fileUri);
        }

    }

    private void sendMessageWithImage(String imageString) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
        message.put(Constant.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constant.KEY_IMAGE, imageString);
        message.put(Constant.KEY_TIMESTAMP, new Date());
        db.collection(Constant.KEY_COLLECTION_CHAT).add(message);

        if (conversionId != null) {
            updateConversion("Image"); // Đây là tin nhắn cho ảnh
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
            conversion.put(Constant.KEY_SENDER_NAME, preferenceManager.getString(Constant.KEY_NAME));
            conversion.put(Constant.KEY_SENDER_IMAGE, preferenceManager.getString(Constant.KEY_IMAGE));
            conversion.put(Constant.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constant.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constant.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constant.KEY_LAST_MESSAGE, binding.inpMsg.getText().toString());
            conversion.put(Constant.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        if (!isReceiverAvailable) {
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Constant.KEY_USER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
                data.put(Constant.KEY_NAME, preferenceManager.getString(Constant.KEY_NAME));
                data.put(Constant.KEY_FCM_TOKEN, preferenceManager.getString(Constant.KEY_FCM_TOKEN));
                data.put(Constant.KEY_MESSAGE, binding.inpMsg.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Constant.REMOTE_MSG_DATA, data);
                body.put(Constant.REMOTE_MSG_REGISTRATION_IDS, tokens);
                sendNotification(body.toString());
            } catch (Exception e) {
                showToast(e.getMessage());
            }
        }
    }


    //File


    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, Constant.PICK_FILE_REQUEST_CODE);

    }

    private void uploadFile(Uri fileUri) {

        String fileName = null;
        if (fileUri.getPath() != null) {
            fileName = new File(fileUri.getPath()).getName();
        } else {
            fileName = UUID.randomUUID().toString();
        }

        StorageReference fileRef = FirebaseStorage.getInstance().getReference().child("uploads").child(fileName);
        final String finalFileName = fileName;

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get a URL to the uploaded content
                    Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!urlTask.isSuccessful()) ;
                    Uri downloadUrl = urlTask.getResult();

                    // Add the file URL and file name to the chat message
                    sendMessageWithFile(downloadUrl.toString(), finalFileName);
                    showToast("Success");
                })
                .addOnFailureListener(exception -> {
                    showToast("Failllll: " + exception);
                    Log.d(TAG, "uploadFile: fail" + exception);
                });
    }


    private void sendMessageWithFile(String fileUrl, String fileName) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
        message.put(Constant.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constant.KEY_FILE_URL, fileUrl);
        message.put(Constant.KEY_FILE_NAME, fileName);
        message.put(Constant.KEY_TIMESTAMP, new Date());
        db.collection(Constant.KEY_COLLECTION_CHAT).add(message);

        if (conversionId != null) {
            updateConversion("File");
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
            conversion.put(Constant.KEY_SENDER_NAME, preferenceManager.getString(Constant.KEY_NAME));
            conversion.put(Constant.KEY_SENDER_IMAGE, preferenceManager.getString(Constant.KEY_IMAGE));
            conversion.put(Constant.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constant.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constant.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constant.KEY_LAST_MESSAGE, "File");
            conversion.put(Constant.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        if (!isReceiverAvailable) {
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Constant.KEY_USER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
                data.put(Constant.KEY_NAME, preferenceManager.getString(Constant.KEY_NAME));
                data.put(Constant.KEY_FCM_TOKEN, preferenceManager.getString(Constant.KEY_FCM_TOKEN));
                data.put(Constant.KEY_MESSAGE, "File"); // Đặt tin nhắn là "File" khi gửi tệp

                JSONObject body = new JSONObject();
                body.put(Constant.REMOTE_MSG_DATA, data);
                body.put(Constant.REMOTE_MSG_REGISTRATION_IDS, tokens);
                sendNotification(body.toString());
            } catch (Exception e) {
                showToast(e.getMessage());
            }
        }
    }


}