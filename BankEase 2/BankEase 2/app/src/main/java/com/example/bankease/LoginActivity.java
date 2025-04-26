package com.example.bankease;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText email, password;
    private Button loginButton;
    private TextView goToRegister;

    private static final String TAG = "LoginActivity";
    private static final int NOTIF_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set fullscreen and adjust UI for immersive experience
        getWindow().setBackgroundDrawableResource(R.drawable.login_bg);

        NotificationHelper.createNotificationChannel(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIF_PERMISSION_CODE);
            } else {
                Log.d(TAG, "🔔 Notification permission already granted.");
            }
        }

        mAuth = FirebaseAuth.getInstance();

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        goToRegister = findViewById(R.id.goToRegister);

        loginButton.setOnClickListener(v -> loginUser());
        goToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String emailStr = email.getText().toString().trim();
        String passwordStr = password.getText().toString().trim();

        if (emailStr.isEmpty() || passwordStr.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!PasswordValidator.isStrongPassword(passwordStr)) {
            NotificationHelper.sendNotification(this,
                    "Weak Password Detected",
                    "Consider updating your password for better security");
        }

        mAuth.signInWithEmailAndPassword(emailStr, passwordStr)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        String uid = user.getUid();
                        Log.d(TAG, "Login successful → UID: " + uid);

                        DatabaseReference userRef = FirebaseDatabase.getInstance()
                                .getReference("Users").child(uid);

                        userRef.child("email").setValue(emailStr);

                        userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists()) {
                                    showNamePrompt(userRef);
                                } else {
                                    proceedToDashboard();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to check name field: " + error.getMessage());
                                proceedToDashboard();
                            }
                        });
                    }
                })
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Login Failed: " + err.getMessage());
                    Toast.makeText(this, "Login Failed: " + err.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showNamePrompt(DatabaseReference userRef) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your name");

        final EditText input = new EditText(this);
        input.setHint("Full Name");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                userRef.child("name").setValue(name)
                        .addOnCompleteListener(task -> proceedToDashboard());
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                proceedToDashboard();
            }
        });

        builder.setNegativeButton("Skip", (dialog, which) -> {
            dialog.cancel();
            proceedToDashboard();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void proceedToDashboard() {
        Log.d(TAG, "Proceeding to DashboardActivity");
        NotificationHelper.sendNotification(this,
                "Login Successful",
                "Welcome back to BankEase!");
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIF_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Notification permission granted.");
            } else {
                Log.w(TAG, "❌ Notification permission denied.");
            }
        }
    }
}