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
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.baitaplon.R;
import com.example.baitaplon.utilities.Constant;
import com.example.baitaplon.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {
    private String encodedImage;
    private PreferenceManager preferenceManager;
    private FirebaseAuth mAuth;
    EditText ipEmail;

    EditText ipName;
    EditText ipPass;
    EditText ipPhone;
    Button btnSignUp;
    TextView back;
    ImageView avatar;
    TextView addAvt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        preferenceManager = new PreferenceManager(getApplicationContext());
        ipName = findViewById(R.id.txtNameSU);
        ipEmail = findViewById(R.id.txEmailSU);
        ipPass = findViewById(R.id.txtPassSU);
        ipPhone = findViewById(R.id.txtPhoneSU);
        btnSignUp = findViewById(R.id.btnSU);
        back = findViewById(R.id.back);
        avatar = findViewById(R.id.avatar);
        addAvt = findViewById(R.id.txtAddAvatar);
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isValidSignUpDetails()){
                    signUp2();
                }

            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                pickImage.launch(intent);
            }
        });
    }
        private void showToast(String msg) {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
        private void signUp(){
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            HashMap<String, Object> user = new HashMap<>();
            user.put(Constant.KEY_NAME, ipName.getText().toString());
            user.put(Constant.KEY_EMAIL, ipEmail.getText().toString());
            user.put(Constant.KEY_PHONE, ipPhone.getText().toString());
            user.put(Constant.KEY_IMAGE, encodedImage);
            db.collection(Constant.KEY_COLLECTION_USERS)
                    .add(user)
                    .addOnSuccessListener(documentReference -> {
                        preferenceManager.putBoolean(Constant.KEY_IS_SIGN_IN, true);
                        preferenceManager.putString(Constant.KEY_USER_ID, documentReference.getId());
                        preferenceManager.putString(Constant.KEY_NAME, ipName.getText().toString());
                        preferenceManager.putString(Constant.KEY_IMAGE, encodedImage);


                        mAuth = FirebaseAuth.getInstance();
                        mAuth.createUserWithEmailAndPassword(ipEmail.getText().toString(), ipPass.getText().toString())
                                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            // Sign in success, update UI with the signed-in user's information
                                            Log.d(TAG, "createUserWithEmail:success");
                                            FirebaseUser user = mAuth.getCurrentUser();

                                        } else {
                                            // If sign in fails, display a message to the user.
                                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                            Toast.makeText(SignUpActivity.this, "Authentication failed.",
                                                    Toast.LENGTH_SHORT).show();

                                        }
                                    }
                                });
                        String hello = "Xin chào " + ipName.getText().toString();
                        Intent intent = new Intent(getApplicationContext(), ResultLoginActivity.class);
                        intent.putExtra("username", hello);

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .addOnFailureListener(exception -> {
                        showToast(exception.getMessage());
                    });
        }

    private void signUp2() {
        mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(ipEmail.getText().toString(), ipPass.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                addUserToFirestore(user.getUid());
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addUserToFirestore(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constant.KEY_NAME, ipName.getText().toString());
        user.put(Constant.KEY_EMAIL, ipEmail.getText().toString());
        user.put(Constant.KEY_PHONE, ipPhone.getText().toString());
        user.put(Constant.KEY_IMAGE, encodedImage);
        db.collection(Constant.KEY_COLLECTION_USERS)
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    preferenceManager.putBoolean(Constant.KEY_IS_SIGN_IN, true);
                    preferenceManager.putString(Constant.KEY_USER_ID, userId);
                    preferenceManager.putString(Constant.KEY_NAME, ipName.getText().toString());
                    preferenceManager.putString(Constant.KEY_IMAGE, encodedImage);
                    String hello = "Xin chào " + ipName.getText().toString();
                    Intent intent = new Intent(getApplicationContext(), ResultLoginActivity.class);
                    intent.putExtra("username", hello);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    showToast(exception.getMessage());
                });
    }


    private String encodedImage(Bitmap bitmap){
            int previewWidth = 150;
            int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
            Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
        }

        private final ActivityResultLauncher pickImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == RESULT_OK) {
                        if(result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                avatar.setImageBitmap(bitmap);
                                 addAvt.setVisibility(View.GONE);
                                 encodedImage = encodedImage(bitmap);
                            }catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );
        private Boolean isValidSignUpDetails() {
            if (encodedImage == null) {
                showToast("Select profile image");
                return false;
            }else if(ipName.getText().toString().trim().isEmpty()) {
                showToast("Enter name");
                return false;
            }
            else if(ipEmail.getText().toString().trim().isEmpty()) {
                showToast("Enter Email");
                return false;
            } else if (!Patterns.EMAIL_ADDRESS.matcher(ipEmail.getText().toString()).matches()) {
                showToast("Enter valid Email");
                return false;
            } else if (ipPass.getText().toString().trim().isEmpty()) {
                showToast("Enter Password");
                return false;
            }else if (ipPhone.getText().toString().trim().isEmpty()) {
                showToast("Enter Phone number");
                return false;
            }else {
                return true;
            }
        }

}