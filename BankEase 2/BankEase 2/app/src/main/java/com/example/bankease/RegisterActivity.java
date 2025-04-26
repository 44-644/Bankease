package com.example.bankease;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, passwordInput, retypePasswordInput;
    private Button registerButton;
    private TextView goToLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameInput = findViewById(R.id.name);
        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        retypePasswordInput = findViewById(R.id.retypePassword);
        registerButton = findViewById(R.id.registerButton);
        goToLogin = findViewById(R.id.goToLogin);

        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("Users");

        registerButton.setOnClickListener(v -> registerUser());

        goToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String retype = retypePasswordInput.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || retype.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(retype)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!PasswordValidator.isStrongPassword(password)) {
            NotificationHelper.sendNotification(this,
                    "Weak Password",
                    "Your password doesn't meet security requirements");

            new AlertDialog.Builder(this)
                    .setTitle("Password Requirements")
                    .setMessage(PasswordValidator.getPasswordRequirements())
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("uid", user.getUid());
                        map.put("name", name);
                        map.put("email", email);

                        userRef.child(user.getUid()).setValue(map)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();
                                    NotificationHelper.sendNotification(this,
                                            "Registration Successful",
                                            "Welcome to BankEase!");
                                    mAuth.signOut();
                                    startActivity(new Intent(this, LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Saving user failed", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, e.getMessage());
                });
    }
}