package com.example.bankease;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TransferSuccessfulActivity extends AppCompatActivity {

    private static final String TAG = "TransferSuccessful";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_successful);

        Log.d(TAG, "✅ TransferSuccessfulActivity started");

        // 🧾 Bind all views
        TextView textDateTime = findViewById(R.id.textDateTime);
        TextView textAmount = findViewById(R.id.textAmount);
        TextView textType = findViewById(R.id.textType);
        TextView textSenderAccount = findViewById(R.id.textSenderAccount);
        TextView textRecipientName = findViewById(R.id.textRecipientName);
        TextView textBeneficiaryAccount = findViewById(R.id.textBeneficiaryAccount);
        TextView textBeneficiaryBank = findViewById(R.id.textBeneficiaryBank);
        TextView textReceiptNo = findViewById(R.id.textReceiptNo);
        TextView textStatus = findViewById(R.id.textStatus);
        Button btnGoToDashboard = findViewById(R.id.btnGoToDashboard);

        // 📦 Get intent extras
        Intent intent = getIntent();
        String amount = intent.getStringExtra("amount");
        String dateTime = intent.getStringExtra("dateTime");
        String type = intent.getStringExtra("transactionType");
        String sender = intent.getStringExtra("senderName");
        String recipientName = intent.getStringExtra("beneficiaryFullName");
        String beneficiaryAcc = intent.getStringExtra("beneficiaryAccount");
        String beneficiaryBank = intent.getStringExtra("beneficiaryBank");
        String receipt = intent.getStringExtra("receipt");

        // 🪵 Debug log
        Log.d(TAG, "📦 Data Received:");
        Log.d(TAG, "Amount: " + amount);
        Log.d(TAG, "Type: " + type);
        Log.d(TAG, "DateTime: " + dateTime);
        Log.d(TAG, "Sender: " + sender);
        Log.d(TAG, "Recipient: " + recipientName);
        Log.d(TAG, "Account: " + beneficiaryAcc);
        Log.d(TAG, "Bank: " + beneficiaryBank);
        Log.d(TAG, "Receipt: " + receipt);

        // 🔄 Set text
        textAmount.setText(amount != null ? amount : "$0.00");
        textDateTime.setText("Generated on: " + (dateTime != null ? dateTime : "N/A"));
        textType.setText(type != null ? type : "INTER-BANK");
        textSenderAccount.setText(sender != null ? sender : "N/A");
        textRecipientName.setText(recipientName != null ? recipientName : "N/A");
        textBeneficiaryAccount.setText(beneficiaryAcc != null ? beneficiaryAcc : "N/A");
        textBeneficiaryBank.setText(beneficiaryBank != null ? beneficiaryBank : "N/A");
        textReceiptNo.setText(receipt != null ? receipt : "N/A");
        textStatus.setText("Transfer Request Successful");

        // 🔙 Return to Dashboard
        btnGoToDashboard.setOnClickListener(v -> {
            Intent intent1 = new Intent(TransferSuccessfulActivity.this, DashboardActivity.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent1);
            finish();
        });
    }
}
