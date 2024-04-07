package com.example.baitaplon.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.baitaplon.databinding.ActivitySigninBinding;
import com.example.baitaplon.utilities.Constant;
import com.example.baitaplon.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class SignInActivity extends AppCompatActivity {
    ActivitySigninBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySigninBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {

                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();

                Log.d(TAG, "onComplete: " + token);
            }
        });

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constant.KEY_IS_SIGN_IN)) {
            Intent intent = new Intent(getApplicationContext(), ResultLoginActivity.class);
            startActivity(intent);
            finish();
        }


        binding.btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isValidSignInDetail()) {
                    signIn2();
                }
            }
        });

        binding.btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signin = new Intent(SignInActivity.this, SignUpActivity.class);
                startActivity(signin);
            }
        });


        binding.forgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText resetMail = new EditText(v.getContext());
                final AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(v.getContext());
                passwordResetDialog.setTitle("Bạn có muốn đặt lại mật khẩu.");
                passwordResetDialog.setMessage("Nhập email của bạn để đặt lại mật khẩu");
                passwordResetDialog.setView(resetMail);

                passwordResetDialog.setPositiveButton("Có", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // extract the email and send reset link
                        String mail = resetMail.getText().toString();
                        FirebaseAuth.getInstance().sendPasswordResetEmail(mail)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "onClick: success " + mail);
                                    Toast.makeText(SignInActivity.this, "Liên kết đặt lại mật khẩu đã được gửi đến email của bạn", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.d(TAG, "onClick: " + mail, e);
                                    Toast.makeText(SignInActivity.this, "Lỗi! Chưa thể gửi được email." + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                });

                passwordResetDialog.setNegativeButton("Không", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // close the dialog
                    }
                });

                passwordResetDialog.create().show();
            }
        });


    }

    private void signIn() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constant.KEY_COLLECTION_USERS)
                .whereEqualTo(Constant.KEY_EMAIL, binding.inpEmail.getText().toString())
                .whereEqualTo(Constant.KEY_PASSWORD, binding.inpPass.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constant.KEY_IS_SIGN_IN, true);
                        preferenceManager.putString(Constant.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constant.KEY_NAME, documentSnapshot.getString(Constant.KEY_NAME));
                        preferenceManager.putString(Constant.KEY_IMAGE, documentSnapshot.getString(Constant.KEY_IMAGE));
                        Intent intent = new Intent(getApplicationContext(), ResultLoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("loginResult", "Đăng nhập thành công");
                        startActivity(intent);
                    } else {
                        showToast("Hiện không thể đăng nhập.");
                    }
                });
    }
    private void signIn2() {
        mAuth.signInWithEmailAndPassword(binding.inpEmail.getText().toString(), binding.inpPass.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                fetchUserData(user.getUid());
                            }
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(SignInActivity.this, "Xác thực người dùng thất bại.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchUserData(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constant.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            preferenceManager.putBoolean(Constant.KEY_IS_SIGN_IN, true);
                            preferenceManager.putString(Constant.KEY_USER_ID, documentSnapshot.getId());
                            preferenceManager.putString(Constant.KEY_NAME, documentSnapshot.getString(Constant.KEY_NAME));
                            preferenceManager.putString(Constant.KEY_IMAGE, documentSnapshot.getString(Constant.KEY_IMAGE));
                            Intent intent = new Intent(getApplicationContext(), ResultLoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.putExtra("loginResult", "Đăng nhập thành công");
                            startActivity(intent);
                        } else {
                            showToast("Không thể tìm thấy dữ liệu người dùng.");
                        }
                    } else {
                        showToast("Không thể tìm thấy dữ liệu người dùng.");
                    }
                });
    }


    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignInDetail() {
        if (binding.inpEmail.getText().toString().trim().isEmpty()) {
            showToast("Nhập email");
            return false;
        } else if (binding.inpPass.getText().toString().trim().isEmpty()) {
            showToast("Nhập mật khẩu");
            return false;
        } else {
            return true;
        }
    }


    private static final String TAG = "SignInActivity";
}
