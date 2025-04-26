package com.example.bankease;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.util.*;

public class FundTransferActivity extends AppCompatActivity {

    private Spinner senderSpinner, receiverSpinner;
    private EditText amountEditText, recipientEmailEditText;
    private Button transferBtn, fetchReceiverBtn;
    private ProgressBar progressBar;

    private DatabaseReference bankRef, userRef, txnRef;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private final List<BankAccount> senderAccounts = new ArrayList<>();
    private final List<BankAccount> receiverAccounts = new ArrayList<>();

    private String recipientEmail = "", recipientName = "", recipientUid = "";

    private static final String TAG = "FundTransferActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fund_transfer);

        senderSpinner = findViewById(R.id.spinnerSender);
        receiverSpinner = findViewById(R.id.spinnerReceiver);
        recipientEmailEditText = findViewById(R.id.recipientEmail);
        fetchReceiverBtn = findViewById(R.id.btnFetchReceiverAccounts);
        amountEditText = findViewById(R.id.editTextAmount);
        transferBtn = findViewById(R.id.btnTransfer);
        progressBar = findViewById(R.id.progressBar);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        bankRef = FirebaseDatabase.getInstance().getReference("BankAccounts");
        userRef = FirebaseDatabase.getInstance().getReference("Users");
        txnRef = FirebaseDatabase.getInstance().getReference("Transactions");

        loadSenderAccounts();

        fetchReceiverBtn.setOnClickListener(v -> fetchReceiverAccounts());
        transferBtn.setOnClickListener(v -> validateAndReviewTransfer());
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
    }

    private void loadSenderAccounts() {
        String uid = currentUser.getUid();
        bankRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                senderAccounts.clear();
                List<String> names = new ArrayList<>();
                for (DataSnapshot bankNode : snapshot.getChildren()) {
                    for (DataSnapshot accSnap : bankNode.getChildren()) {
                        BankAccount acc = accSnap.getValue(BankAccount.class);
                        if (acc != null && uid.equals(acc.getUserId())) {
                            acc.setAccountId(accSnap.getKey());
                            senderAccounts.add(acc);
                            names.add(acc.getBankName() + " - " + acc.getAccountNumber());
                        }
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(FundTransferActivity.this,
                        android.R.layout.simple_spinner_item, names);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                senderSpinner.setAdapter(adapter);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FundTransferActivity.this, "Failed to load sender accounts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchReceiverAccounts() {
        String inputEmail = recipientEmailEditText.getText().toString().trim().toLowerCase();
        if (inputEmail.isEmpty()) {
            Toast.makeText(this, "Enter recipient email", Toast.LENGTH_SHORT).show();
            return;
        }

        userRef.orderByChild("email").equalTo(inputEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(FundTransferActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    recipientEmail = inputEmail;
                    recipientName = userSnap.child("name").getValue(String.class);
                    recipientUid = userSnap.getKey();
                    break;
                }

                loadReceiverAccounts(recipientUid);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FundTransferActivity.this, "Failed to fetch user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReceiverAccounts(String recipientUid) {
        bankRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                receiverAccounts.clear();
                List<String> names = new ArrayList<>();
                for (DataSnapshot bankNode : snapshot.getChildren()) {
                    for (DataSnapshot accSnap : bankNode.getChildren()) {
                        BankAccount acc = accSnap.getValue(BankAccount.class);
                        if (acc != null && recipientUid.equals(acc.getUserId())) {
                            acc.setAccountId(accSnap.getKey());
                            receiverAccounts.add(acc);
                            names.add(acc.getBankName() + " - " + acc.getAccountNumber());
                        }
                    }
                }

                if (receiverAccounts.isEmpty()) {
                    Toast.makeText(FundTransferActivity.this, "No accounts found for recipient", Toast.LENGTH_SHORT).show();
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(FundTransferActivity.this,
                        android.R.layout.simple_spinner_item, names);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                receiverSpinner.setAdapter(adapter);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FundTransferActivity.this, "Failed to load receiver accounts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateAndReviewTransfer() {
        if (senderAccounts.isEmpty() || receiverAccounts.isEmpty()) {
            Toast.makeText(this, "Please load sender and receiver accounts", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = amountEditText.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        int senderIndex = senderSpinner.getSelectedItemPosition();
        int receiverIndex = receiverSpinner.getSelectedItemPosition();

        BankAccount sender = senderAccounts.get(senderIndex);
        BankAccount receiver = receiverAccounts.get(receiverIndex);

        String senderAccountStr = sender.getBankName() + " - " + sender.getAccountNumber();

        // ✅ Notify sender that transfer review is initiated
        NotificationHelper.sendNotification(
                FundTransferActivity.this,
                "Transfer Initiated",
                "You are sending $" + amountStr + " to " + (recipientName != null ? recipientName : "recipient")
        );

        // ➡️ Proceed to review screen
        Intent intent = new Intent(this, ReviewTransferActivity.class);
        intent.putExtra("recipientName", recipientName != null ? recipientName : "Recipient");
        intent.putExtra("recipientEmail", recipientEmail);
        intent.putExtra("amount", amountStr);
        intent.putExtra("senderAccount", senderAccountStr);
        intent.putExtra("senderBankKey", sender.getBankName().replace(" ", "_"));
        intent.putExtra("senderAccountId", sender.getAccountId());
        intent.putExtra("receiverBankKey", receiver.getBankName().replace(" ", "_"));
        intent.putExtra("receiverAccountId", receiver.getAccountId());

        startActivity(intent);
    }

}
