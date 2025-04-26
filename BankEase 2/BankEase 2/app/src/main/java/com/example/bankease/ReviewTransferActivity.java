package com.example.bankease;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReviewTransferActivity extends AppCompatActivity {

    private static final String TAG = "ReviewTransferActivity";

    private TextView tvRecipientName, tvRecipientEmail, tvAmount, tvSenderAccount, tvDate;
    private Button btnConfirmSend, btnCancel;

    private String recipientName, recipientEmail, amountStr, senderAccountStr;
    private String senderBankKey, senderAccountId;
    private String receiverBankKey, receiverAccountId;

    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_transfer);

        Log.d(TAG, "🔄 onCreate: Binding UI");

        tvRecipientName = findViewById(R.id.tvRecipientName);
        tvRecipientEmail = findViewById(R.id.tvRecipientEmail);
        tvAmount = findViewById(R.id.tvAmount);
        tvSenderAccount = findViewById(R.id.tvSenderAccount);
        tvDate = findViewById(R.id.tvDate);
        btnConfirmSend = findViewById(R.id.btnConfirmSend);
        btnCancel = findViewById(R.id.btnCancel);

        dbRef = FirebaseDatabase.getInstance().getReference();

        Log.d(TAG, "📦 Receiving Intent extras");
        Intent intent = getIntent();
        recipientName = intent.getStringExtra("recipientName");
        recipientEmail = intent.getStringExtra("recipientEmail");
        amountStr = intent.getStringExtra("amount");
        senderAccountStr = intent.getStringExtra("senderAccount");
        senderBankKey = intent.getStringExtra("senderBankKey");
        senderAccountId = intent.getStringExtra("senderAccountId");
        receiverBankKey = intent.getStringExtra("receiverBankKey");
        receiverAccountId = intent.getStringExtra("receiverAccountId");

        if (senderBankKey == null || senderAccountId == null || receiverBankKey == null || receiverAccountId == null) {
            Log.e(TAG, "❌ Missing critical transfer info. Aborting.");
            Toast.makeText(this, "Transfer setup failed. Missing account info.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "✅ Intent extras: " +
                "\nRecipient: " + recipientName +
                "\nSenderAccount: " + senderAccountStr +
                "\nAmount: " + amountStr);

        tvRecipientName.setText(recipientName != null ? recipientName : "Unknown");
        tvRecipientEmail.setText(recipientEmail != null ? recipientEmail : "N/A");
        tvAmount.setText(amountStr != null ? "$" + amountStr : "$0.00");
        tvSenderAccount.setText(senderAccountStr != null ? senderAccountStr : "Account Info");
        tvDate.setText("Today");
        btnConfirmSend.setText("Send $" + amountStr);

        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "🚫 Cancel clicked");
            finish();
        });

        btnConfirmSend.setOnClickListener(v -> {
            Log.d(TAG, "✅ Confirm Transfer clicked");
            performFinalTransfer();
        });
    }

    private void performFinalTransfer() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Processing...");
        dialog.setCancelable(false);
        dialog.show();

        Log.d(TAG, "📡 Fetching sender and receiver account info...");
        DatabaseReference senderRef = dbRef.child("BankAccounts").child(senderBankKey).child(senderAccountId);
        DatabaseReference receiverRef = dbRef.child("BankAccounts").child(receiverBankKey).child(receiverAccountId);

        senderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot senderSnap) {
                BankAccount sender = senderSnap.getValue(BankAccount.class);
                if (sender == null) {
                    dialog.dismiss();
                    Log.e(TAG, "❌ Sender account not found");
                    Toast.makeText(ReviewTransferActivity.this, "Sender account not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                receiverRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot receiverSnap) {
                        BankAccount receiver = receiverSnap.getValue(BankAccount.class);
                        if (receiver == null) {
                            dialog.dismiss();
                            Log.e(TAG, "❌ Receiver account not found");
                            Toast.makeText(ReviewTransferActivity.this, "Receiver account not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double amount = Double.parseDouble(amountStr);
                        double senderBal = Double.parseDouble(sender.getCurrentBalance());
                        double receiverBal = Double.parseDouble(receiver.getCurrentBalance());

                        if (senderBal < amount) {
                            dialog.dismiss();
                            Log.w(TAG, "⚠️ Insufficient balance");
                            Toast.makeText(ReviewTransferActivity.this, "Insufficient balance", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Map<String, Object> updateMap = new HashMap<>();
                        updateMap.put("BankAccounts/" + senderBankKey + "/" + senderAccountId + "/currentBalance", String.valueOf(senderBal - amount));
                        updateMap.put("BankAccounts/" + receiverBankKey + "/" + receiverAccountId + "/currentBalance", String.valueOf(receiverBal + amount));

                        Log.d(TAG, "💾 Updating balances in Firebase...");
                        dbRef.updateChildren(updateMap)
                                .addOnSuccessListener(unused -> {
                                    logTransaction(sender, receiver, amount);
                                    dialog.dismiss();

                                    Log.d(TAG, "✅ Transfer successful, showing notifications");
                                    NotificationHelper.sendNotification(
                                            ReviewTransferActivity.this,
                                            "✅ Transfer Successful",
                                            "You sent $" + amount + " to " + recipientName
                                    );

                                    NotificationHelper.sendNotification(
                                            ReviewTransferActivity.this,
                                            "💰 You've Received Funds",
                                            "You received $" + amount + " from " + sender.getBankName()
                                    );

                                    String receiptNo = "N" + UUID.randomUUID().toString().substring(0, 10).replace("-", "").toUpperCase();
                                    String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                                    Log.d(TAG, "📄 Launching TransferSuccessfulActivity with receipt info:");
                                    Log.d(TAG, "→ Amount: $" + amount);
                                    Log.d(TAG, "→ Receipt: " + receiptNo);
                                    Log.d(TAG, "→ Date: " + dateTime);
                                    Log.d(TAG, "→ Sender: " + sender.getBankName() + " - " + sender.getAccountNumber());
                                    Log.d(TAG, "→ Receiver: " + receiver.getBankName() + " - " + receiver.getAccountNumber());

                                    Intent successIntent = new Intent(ReviewTransferActivity.this, TransferSuccessfulActivity.class);
                                    successIntent.putExtra("amount", "$" + amount);
                                    successIntent.putExtra("receipt", receiptNo);
                                    successIntent.putExtra("dateTime", dateTime);
                                    successIntent.putExtra("senderName", sender.getBankName() + " - " + sender.getAccountNumber());
                                    successIntent.putExtra("beneficiaryFullName", recipientName);
                                    successIntent.putExtra("beneficiaryAccount", receiver.getAccountNumber());
                                    successIntent.putExtra("beneficiaryBank", receiver.getBankName());
                                    successIntent.putExtra("transactionType", "INTER-BANK");

                                    startActivity(successIntent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    dialog.dismiss();
                                    Log.e(TAG, "❌ Firebase update failed: " + e.getMessage());
                                    Toast.makeText(ReviewTransferActivity.this, "Transfer failed", Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        dialog.dismiss();
                        Log.e(TAG, "❌ Receiver fetch error: " + error.getMessage());
                        Toast.makeText(ReviewTransferActivity.this, "Failed to fetch receiver", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                dialog.dismiss();
                Log.e(TAG, "❌ Sender fetch error: " + error.getMessage());
                Toast.makeText(ReviewTransferActivity.this, "Failed to fetch sender", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logTransaction(BankAccount sender, BankAccount receiver, double amount) {
        Log.d(TAG, "📝 Logging transaction to Firebase...");
        Map<String, Object> txn = new HashMap<>();
        txn.put("fromAccount", sender.getBankName() + "-" + sender.getAccountNumber());
        txn.put("toAccount", receiver.getBankName() + "-" + receiver.getAccountNumber());
        txn.put("amount", amount);
        txn.put("timestamp", System.currentTimeMillis());

        dbRef.child("Transactions").push().setValue(txn);
    }
}
