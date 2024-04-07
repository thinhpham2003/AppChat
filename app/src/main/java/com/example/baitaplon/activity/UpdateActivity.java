package com.example.baitaplon.activity;

import static android.content.ContentValues.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.baitaplon.R;
import com.example.baitaplon.databinding.ActivitySigninBinding;
import com.example.baitaplon.databinding.ActivityUpdateBinding;
import com.example.baitaplon.utilities.Constant;
import com.example.baitaplon.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class UpdateActivity extends AppCompatActivity {
    private String encodedImage;
    private PreferenceManager preferenceManager;
    private FirebaseAuth mAuth;

    ActivityUpdateBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection(Constant.KEY_COLLECTION_USERS).document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                binding.txtNameSU.setText(document.getString(Constant.KEY_NAME));
                                binding.txEmailSU.setText(document.getString(Constant.KEY_EMAIL));
                                binding.txtPhoneSU.setText(document.getString(Constant.KEY_PHONE));

                                encodedImage = document.getString(Constant.KEY_IMAGE);
                                byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                binding.avatar.setImageBitmap(decodedByte);

                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    }
                });

        binding.btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Start");
                binding.progressBar.setVisibility(View.VISIBLE);
                update();
            }
        });
        binding.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        binding.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                pickImage.launch(intent);
            }
        });
    }

    private final ActivityResultLauncher pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.avatar.setImageBitmap(bitmap);
                            encodedImage = encodedImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private String encodedImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
    }

    private boolean isUpdating = false;

    private void update() {

        if (isUpdating) {
            return;
        }
        isUpdating = true;  // Set flag to true

        String name = binding.txtNameSU.getText().toString().trim();
        String email = binding.txEmailSU.getText().toString().trim();
        String phone = binding.txtPhoneSU.getText().toString().trim();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put(Constant.KEY_NAME, name);
        user.put(Constant.KEY_EMAIL, email);
        user.put(Constant.KEY_PHONE, phone);
        user.put(Constant.KEY_IMAGE, encodedImage);

        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection(Constant.KEY_COLLECTION_USERS).document(userId)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        isUpdating = false;  // Reset flag
                        binding.progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onSuccess: Doneee");
                            Toast.makeText(UpdateActivity.this, "Thông tin người dùng được cập nhật thành công.", Toast.LENGTH_SHORT).show();
                            preferenceManager.putString(Constant.KEY_NAME, name);
                            preferenceManager.putString(Constant.KEY_IMAGE, encodedImage);
                        } else {
                            Log.w(TAG, "onFailure", task.getException());
                            String message = "Không thể cập nhật thông tin người dùng";
                            if (task.getException() instanceof FirebaseAuthException) {
                                message = ((FirebaseAuthException) task.getException()).getMessage();
                            }
                            Toast.makeText(UpdateActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }






}
