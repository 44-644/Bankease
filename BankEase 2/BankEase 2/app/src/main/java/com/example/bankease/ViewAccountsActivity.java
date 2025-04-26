package com.example.bankease;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ViewAccountsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BankAccountAdapter adapter;
    private final List<BankAccount> accountList = new ArrayList<>();
    private FirebaseUser user;
    private DatabaseReference dbRef;
    private static final String TAG = "ViewAccountsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_accounts);
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());


        recyclerView = findViewById(R.id.accountsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BankAccountAdapter(this, accountList);
        recyclerView.setAdapter(adapter);

        user = FirebaseAuth.getInstance().getCurrentUser();
        dbRef = FirebaseDatabase.getInstance().getReference("BankAccounts");

        if (user != null) {
            Log.d(TAG, "Fetching accounts for UID: " + user.getUid());
            fetchAccounts();
        } else {
            Log.e(TAG, "User not logged in.");
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchAccounts() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                accountList.clear();

                for (DataSnapshot bankSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot accountSnapshot : bankSnapshot.getChildren()) {
                        BankAccount acc = accountSnapshot.getValue(BankAccount.class);
                        if (acc != null && acc.getUserId() != null &&
                                acc.getUserId().equals(user.getUid())) {

                            // ✅ Set the accountId (Firebase push key)
                            acc.setAccountId(accountSnapshot.getKey());

                            accountList.add(acc);
                            Log.d(TAG, "Found account: " + acc.getBankName() + ", " + acc.getAccountNumber());
                        }
                    }
                }

                if (accountList.isEmpty()) {
                    Log.d(TAG, "No accounts found for user.");
                    Toast.makeText(ViewAccountsActivity.this, "No linked accounts found.", Toast.LENGTH_SHORT).show();
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
                Toast.makeText(ViewAccountsActivity.this, "Failed to load accounts", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
