package com.example.baitaplon.activity;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.example.baitaplon.adapters.RecentConversationsAdapter;
import com.example.baitaplon.databinding.ActivityResultLoginBinding;
import com.example.baitaplon.listener.ConversionListener;
import com.example.baitaplon.models.Message;
import com.example.baitaplon.models.User;
import com.example.baitaplon.utilities.Constant;
import com.example.baitaplon.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ResultLoginActivity extends BaseActivity implements ConversionListener {

    private ActivityResultLoginBinding binding;
    private PreferenceManager preferenceManager;
    private List<Message> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultLoginBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
        setListener();
        listenConversations();
    }

    private void init() {
        conversations = new ArrayList<>();
        conversationsAdapter = new RecentConversationsAdapter(conversations, this);
        binding.cvsRecyclerView.setAdapter(conversationsAdapter);
        db = FirebaseFirestore.getInstance();
    }

    private void setListener() {
        binding.imgSignOut.setOnClickListener(v -> signOut());
        binding.fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), UserActivity.class)));
        binding.imgUpdate.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), UpdateActivity.class)));
    }


    private void loadUserDetails() {
        binding.txtName.setText(preferenceManager.getString(Constant.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constant.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.avatarProfile.setImageBitmap(bitmap);

    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void listenConversations() {
        db.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        db.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constant.KEY_RECEIVER_ID, preferenceManager.getString(Constant.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
       if(error != null) {
           return;
       }
       if(value != null) {
           for (DocumentChange documentChange : value.getDocumentChanges()) {
               if(documentChange.getType() == DocumentChange.Type.ADDED) {
                   String senderId = documentChange.getDocument().getString(Constant.KEY_SENDER_ID);
                   String receiverId = documentChange.getDocument().getString(Constant.KEY_RECEIVER_ID);
                   Message message = new Message();
                   message.senderId = senderId;
                   message.receiverId = receiverId;
                   if(preferenceManager.getString(Constant.KEY_USER_ID).equals(senderId)) {
                       message.conversionImage = documentChange.getDocument().getString(Constant.KEY_RECEIVER_IMAGE);
                       message.conversionName = documentChange.getDocument().getString(Constant.KEY_RECEIVER_NAME);
                       message.conversionId = documentChange.getDocument().getString(Constant.KEY_RECEIVER_ID);
                   }
                   else {
                       message.conversionImage = documentChange.getDocument().getString(Constant.KEY_SENDER_IMAGE);
                       message.conversionName = documentChange.getDocument().getString(Constant.KEY_SENDER_NAME);
                       message.conversionId = documentChange.getDocument().getString(Constant.KEY_SENDER_ID);
                   }

                   message.message = documentChange.getDocument().getString(Constant.KEY_LAST_MESSAGE);
                   message.dateObject = documentChange.getDocument().getDate(Constant.KEY_TIMESTAMP);
                   conversations.add(message);
               } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                   for (int i = 0; i < conversations.size(); i++) {
                       String senderId = documentChange.getDocument().getString(Constant.KEY_SENDER_ID);
                       String receiverId = documentChange.getDocument().getString(Constant.KEY_RECEIVER_ID);
                       if(conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)) {
                           conversations.get(i).message = documentChange.getDocument().getString(Constant.KEY_LAST_MESSAGE);
                           conversations.get(i).dateObject = documentChange.getDocument().getDate(Constant.KEY_TIMESTAMP);
                           break;
                       }
                   }
               }
           }
           Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
           conversationsAdapter.notifyDataSetChanged();
           binding.cvsRecyclerView.smoothScrollToPosition(0);
           binding.cvsRecyclerView.setVisibility(View.VISIBLE);
           binding.prgBar.setVisibility(View.GONE);
       }
    });
    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        preferenceManager.putString(Constant.KEY_FCM_TOKEN, token);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                db.collection(Constant.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constant.KEY_USER_ID));
        documentReference.update(Constant.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Không thể cập nhật token"));
    }

    private void signOut() {
        showToast("Đăng xuất...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                db.collection(Constant.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constant.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constant.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Không thể đăng xuất"));
    }

    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constant.KEY_USER, user);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        preferenceManager = new PreferenceManager(getApplicationContext());
        loadUserDetails();
        getToken();
    }

}