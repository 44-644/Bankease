package com.example.bankease;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class AddBankActivity extends AppCompatActivity {

    Spinner bankSpinner;
    EditText accountNumber, accountType, currentBalance;
    Button linkAccountBtn;
    ImageButton backBtn;
    FirebaseUser user;

    private static final String TAG = "AddBankActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bank);

        Log.d(TAG, "Activity started");

        // UI bindings
        bankSpinner = findViewById(R.id.bankSpinner);
        accountNumber = findViewById(R.id.accountNumber);
        accountType = findViewById(R.id.accountType);
        currentBalance = findViewById(R.id.currentBalance);
        linkAccountBtn = findViewById(R.id.linkAccountBtn);
        backBtn = findViewById(R.id.backBtn);

        // Spinner setup
        String[] banks = {"Select Bank", "Bank of America", "Wells Fargo", "Chase", "CitiBank"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, banks);
        bankSpinner.setAdapter(adapter);

        // Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "FirebaseUser is null! You must be logged in.");
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Link Account Button
        linkAccountBtn.setOnClickListener(v -> {
            String bankName = bankSpinner.getSelectedItem().toString().trim();
            String accNo = accountNumber.getText().toString().trim();
            String accType = accountType.getText().toString().trim();
            String balance = currentBalance.getText().toString().trim();

            Log.d(TAG, "Form data — Bank: " + bankName + ", Acc#: " + accNo + ", Type: " + accType + ", Balance: " + balance);

            if (!bankName.equals("Select Bank") && !accNo.isEmpty() && !balance.isEmpty()) {
                DatabaseReference bankRef = FirebaseDatabase.getInstance()
                        .getReference("BankAccounts")
                        .child(bankName.replace(" ", "_"));

                String id = bankRef.push().getKey();

                Map<String, Object> account = new HashMap<>();
                account.put("accountId", id);
                account.put("bankName", bankName);
                account.put("accountNumber", accNo);
                account.put("accountType", accType);
                account.put("currentBalance", balance);
                account.put("userId", user.getUid());

                bankRef.child(id).setValue(account)
                        .addOnSuccessListener(task -> {
                            Log.d(TAG, "Account linked successfully");
                            Toast.makeText(this, "✅ Account linked under " + bankName, Toast.LENGTH_SHORT).show();

                            // 🔔 Send notification
                            NotificationHelper.sendNotification(
                                    AddBankActivity.this,
                                    "Bank Account Added",
                                    "Your new account was successfully linked!"
                            );
                            Log.d("AddBankActivity", "Notification triggered after account addition");


                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firebase write failed", e);
                            Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                Log.w(TAG, "Validation failed - some fields are missing or 'Select Bank' was chosen.");
                Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔙 Back Button
        backBtn.setOnClickListener(v -> finish());
    }
}
