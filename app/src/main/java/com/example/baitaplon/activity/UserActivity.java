package com.example.baitaplon.activity;


import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.SearchView;

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

    }

    private void setListeners() {
        binding.imgBack.setOnClickListener(v -> onBackPressed());
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loading(true);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                loading(true);
                if (!newText.isEmpty()) {
                    getUser(newText);
                }
                return true;
            }
        });
    }

    private void getUser(String searchText) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constant.KEY_COLLECTION_USERS)
                .whereEqualTo(Constant.KEY_NAME, searchText)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
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
                            binding.txtErrorMsg.setVisibility(View.GONE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }

    private void showErrorMessage() {
        binding.txtErrorMsg.setText(String.format("%s", "Không tìm thấy người dùng"));
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
        Log.d(TAG, "onUserClicked: " + user);
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constant.KEY_USER, user);
        startActivity(intent);
        finish();
    }
}