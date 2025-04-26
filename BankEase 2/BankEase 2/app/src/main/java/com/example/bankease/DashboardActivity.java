package com.example.bankease;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;


public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private TextView welcomeText, accountSummary;
    private Button addBankBtn, transferBtn, reportBtn, accountsBtn, logoutBtn;

    private FirebaseUser user;
    private DatabaseReference bankRef, userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);  // ✅ Make sure layout has all IDs

        Log.d(TAG, "🔁 onCreate started");

        // 🔗 UI Binding
        welcomeText = findViewById(R.id.welcomeText);
        accountSummary = findViewById(R.id.accountSummary);
        addBankBtn = findViewById(R.id.btnAddBank);
        transferBtn = findViewById(R.id.btnTransferFunds);
        reportBtn = findViewById(R.id.btnReports);
        accountsBtn = findViewById(R.id.btnViewAccounts);
        logoutBtn = findViewById(R.id.btnLogout);
      //  Button testNotifBtn = findViewById(R.id.btnTestNotification);

        // ✅ Check if all UI elements exist
        if (logoutBtn == null) {
            Log.e(TAG, "❌ btnLogout not found. Make sure it's in activity_dashboard.xml");
            Toast.makeText(this, "Logout button missing!", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "✅ Logout button found");
        }


        // 🔐 Firebase setup
        user = FirebaseAuth.getInstance().getCurrentUser();
        userRef = FirebaseDatabase.getInstance().getReference("Users");
        bankRef = FirebaseDatabase.getInstance().getReference("BankAccounts");

        if (user != null) {
            fetchUserNameAndDisplay();
            fetchAccountSummary();
        } else {
            welcomeText.setText("👋 Welcome!");
            Log.w(TAG, "⚠️ Firebase user is null");
        }

        // 🚀 Button Actions
        addBankBtn.setOnClickListener(v -> startActivity(new Intent(this, AddBankActivity.class)));
        transferBtn.setOnClickListener(v -> startActivity(new Intent(this, FundTransferActivity.class)));
        reportBtn.setOnClickListener(v -> startActivity(new Intent(this, ExpenseReportActivity.class)));
        accountsBtn.setOnClickListener(v -> startActivity(new Intent(this, ViewAccountsActivity.class)));

        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> {
                Log.d(TAG, "🚪 Logout clicked");
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        }

        Log.d(TAG, "✅ DashboardActivity setup complete");
    }

    private void fetchUserNameAndDisplay() {
        userRef.child(user.getUid()).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                String name = snapshot.getValue(String.class);
                welcomeText.setText(name != null && !name.isEmpty() ? "👋 Welcome, " + name + "!" : "👋 Welcome!");
            }

            @Override public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to fetch name: " + error.getMessage());
                welcomeText.setText("👋 Welcome!");
            }
        });
    }

    private void fetchAccountSummary() {
        bankRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                int count = 0;
                double total = 0;

                for (DataSnapshot bankSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot accountSnapshot : bankSnapshot.getChildren()) {
                        BankAccount account = accountSnapshot.getValue(BankAccount.class);
                        if (account != null && user.getUid().equals(account.getUserId())) {
                            count++;
                            try {
                                total += Double.parseDouble(account.getCurrentBalance());
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid balance for: " + account.getAccountNumber());
                            }
                        }
                    }
                }

                accountSummary.setText("🏦 " + count + " Accounts | 💰 $" + total);
            }

            @Override public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to fetch accounts: " + error.getMessage());
                accountSummary.setText("Error loading balance");
            }
        });
    }
}
