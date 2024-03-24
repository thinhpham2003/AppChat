package com.example.baitaplon.activity;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.baitaplon.adapters.UsersAdapter;
import com.example.baitaplon.databinding.ActivityUserBinding;
import com.example.baitaplon.listener.UserListener;
import com.example.baitaplon.models.User;
import com.example.baitaplon.utilities.Constant;
import com.example.baitaplon.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.util.ArrayList;
import java.util.List;

public class UserActivity extends BaseActivity implements UserListener {

    private ActivityUserBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        getUser();
    }

    private void setListeners() {
        binding.imgBack.setOnClickListener(v -> onBackPressed());
    }

    private void getUser() {
        loading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constant.KEY_COLLECTION_USERS).get().addOnCompleteListener(task -> {
            loading(false);
            String currentUserId = preferenceManager.getString(Constant.KEY_USER_ID);
            if (task.isSuccessful() && task.getResult() != null) {
                List<User> users = new ArrayList<>();
                for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                    if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                        continue;
                    }
                    User user = new User();
                    user.name = queryDocumentSnapshot.getString(Constant.KEY_NAME);
                    user.email = queryDocumentSnapshot.getString(Constant.KEY_EMAIL);
                    user.image = queryDocumentSnapshot.getString(Constant.KEY_IMAGE);
                    user.token = queryDocumentSnapshot.getString(Constant.KEY_FCM_TOKEN);
                    user.id = queryDocumentSnapshot.getId();
                    users.add(user);
                }
                if (users.size() > 0) {
                    UsersAdapter usersAdapter = new UsersAdapter(users, this);
                    binding.usersRecycleView.setAdapter(usersAdapter);
                    binding.usersRecycleView.setVisibility(View.VISIBLE);
                } else {
                    showErrorMessage();
                }
            } else {
                showErrorMessage();
            }
        });
    }

    private void showErrorMessage() {
        binding.txtErrorMsg.setText(String.format("%s", "No user available"));
        binding.txtErrorMsg.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.prgbar.setVisibility(View.VISIBLE);

        } else {
            binding.prgbar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constant.KEY_USER, user);
        startActivity(intent);
        finish();
    }
}