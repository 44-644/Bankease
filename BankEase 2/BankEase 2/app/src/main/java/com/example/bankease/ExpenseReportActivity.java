package com.example.bankease;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class    ExpenseReportActivity extends AppCompatActivity {

    private BarChart balanceChart, interBankChart;
    private PieChart pieChart;
    private FirebaseAuth auth;
    private DatabaseReference bankRef, txnRef;

    private static final String TAG = "ExpenseReportActivity";

    public static class Transaction {
        public String fromAccount, toAccount;
        public double amount;
        public long timestamp;
        public Transaction() {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_report);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        // Firebase init BEFORE any use
        auth = FirebaseAuth.getInstance();
        bankRef = FirebaseDatabase.getInstance().getReference("BankAccounts");
        txnRef = FirebaseDatabase.getInstance().getReference("Transactions");

        balanceChart = findViewById(R.id.balanceChart);
        interBankChart = findViewById(R.id.interBankChart);
        pieChart = findViewById(R.id.topTxnPie);
        BarChart sentReceivedChart = findViewById(R.id.sentReceivedChart);

        loadBalanceData();
        loadTopTransactions();
        loadInterBankTransfers();
        loadMonthlySentReceived(sentReceivedChart); // Move this AFTER auth init ✅
    }


    private void loadBalanceData() {
        String userId = auth.getCurrentUser().getUid();
        Map<String, Float> accountBalances = new HashMap<>();

        bankRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot bankNode : snapshot.getChildren()) {
                    for (DataSnapshot accSnap : bankNode.getChildren()) {
                        BankAccount acc = accSnap.getValue(BankAccount.class);
                        if (acc != null && acc.getUserId().equals(userId)) {
                            accountBalances.put(acc.getBankName() + "-" + acc.getAccountNumber(),
                                    Float.parseFloat(acc.getCurrentBalance()));
                        }
                    }
                }

                List<BarEntry> entries = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                int i = 0;
                for (Map.Entry<String, Float> entry : accountBalances.entrySet()) {
                    entries.add(new BarEntry(i, entry.getValue()));
                    labels.add(entry.getKey());
                    i++;
                }

                BarDataSet dataSet = new BarDataSet(entries, "Account Balances");
                dataSet.setColor(getResources().getColor(R.color.purple_500));
                BarData data = new BarData(dataSet);
                balanceChart.setData(data);
                XAxis xAxis = balanceChart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setGranularity(1f);
                xAxis.setLabelRotationAngle(-45f);
                balanceChart.invalidate();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ExpenseReportActivity.this, "Failed to load balances", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTopTransactions() {
        txnRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Transaction> allTxns = new ArrayList<>();
                for (DataSnapshot txnSnap : snapshot.getChildren()) {
                    Transaction txn = txnSnap.getValue(Transaction.class);
                    if (txn != null) allTxns.add(txn);
                }

                allTxns.sort((a, b) -> Double.compare(b.amount, a.amount));
                List<Transaction> top5 = allTxns.subList(0, Math.min(5, allTxns.size()));

                List<PieEntry> entries = new ArrayList<>();
                for (Transaction txn : top5) {
                    String label = txn.fromAccount + " → " + txn.toAccount;
                    entries.add(new PieEntry((float) txn.amount, label));
                }

                PieDataSet dataSet = new PieDataSet(entries, "Top Transactions");
                dataSet.setColors(new int[]{R.color.purple_500, R.color.teal_700, R.color.black, R.color.white}, getApplicationContext());
                PieData data = new PieData(dataSet);
                pieChart.setData(data);
                pieChart.setCenterText("Top 5 Transfers");
                pieChart.setEntryLabelTextSize(10f);
                pieChart.invalidate();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load transactions", error.toException());
            }
        });
    }

    private void loadInterBankTransfers() {
        txnRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Float> transferMap = new HashMap<>();
                for (DataSnapshot txnSnap : snapshot.getChildren()) {
                    String from = txnSnap.child("fromAccount").getValue(String.class);
                    String to = txnSnap.child("toAccount").getValue(String.class);
                    Double amount = txnSnap.child("amount").getValue(Double.class);
                    if (from != null && to != null && amount != null && !from.equals(to)) {
                        String fromBank = from.split("-")[0].trim();
                        String toBank = to.split("-")[0].trim();
                        String key = fromBank + "→" + toBank;
                        transferMap.put(key, transferMap.getOrDefault(key, 0f) + amount.floatValue());
                    }
                }

                List<BarEntry> entries = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                int i = 0;
                for (Map.Entry<String, Float> entry : transferMap.entrySet()) {
                    entries.add(new BarEntry(i, entry.getValue()));
                    labels.add(entry.getKey());
                    i++;
                }

                BarDataSet dataSet = new BarDataSet(entries, "Inter-Bank Transfers");
                BarData data = new BarData(dataSet);
                interBankChart.setData(data);
                XAxis xAxis = interBankChart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setLabelRotationAngle(-45f);
                xAxis.setGranularity(1f);
                interBankChart.invalidate();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load inter-bank transfers", error.toException());
            }
        });
    }
    private void loadMonthlySentReceived(BarChart chart) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Set<String> userAccounts = new HashSet<>();

        // Step 1: Get all account numbers for this user
        bankRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot bankNode : snapshot.getChildren()) {
                    for (DataSnapshot accSnap : bankNode.getChildren()) {
                        BankAccount acc = accSnap.getValue(BankAccount.class);
                        if (acc != null && userId.equals(acc.getUserId())) {
                            userAccounts.add(acc.getBankName() + "-" + acc.getAccountNumber());
                        }
                    }
                }

                // Step 2: Read transactions and classify
                txnRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Float> sentMap = new HashMap<>();
                        Map<String, Float> receivedMap = new HashMap<>();

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Transaction txn = snap.getValue(Transaction.class);
                            if (txn == null) continue;

                            String month = new java.text.SimpleDateFormat("MMM yyyy", Locale.US)
                                    .format(new java.util.Date(txn.timestamp));

                            if (userAccounts.contains(txn.fromAccount)) {
                                sentMap.put(month, sentMap.getOrDefault(month, 0f) + (float) txn.amount);
                            }
                            if (userAccounts.contains(txn.toAccount)) {
                                receivedMap.put(month, receivedMap.getOrDefault(month, 0f) + (float) txn.amount);
                            }
                        }

                        Set<String> allMonths = new TreeSet<>(sentMap.keySet());
                        allMonths.addAll(receivedMap.keySet());

                        List<BarEntry> sentEntries = new ArrayList<>();
                        List<BarEntry> receivedEntries = new ArrayList<>();
                        List<String> months = new ArrayList<>();
                        int i = 0;
                        for (String month : allMonths) {
                            months.add(month);
                            sentEntries.add(new BarEntry(i, sentMap.getOrDefault(month, 0f)));
                            receivedEntries.add(new BarEntry(i, receivedMap.getOrDefault(month, 0f)));
                            i++;
                        }

                        BarDataSet sentSet = new BarDataSet(sentEntries, "Sent");
                        BarDataSet recvSet = new BarDataSet(receivedEntries, "Received");
                        sentSet.setColor(getResources().getColor(R.color.purple_500));
                        recvSet.setColor(getResources().getColor(R.color.teal_700));

                        BarData data = new BarData(sentSet, recvSet);
                        data.setBarWidth(0.4f);
                        chart.setData(data);

                        XAxis xAxis = chart.getXAxis();
                        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        xAxis.setGranularity(1f);
                        xAxis.setCenterAxisLabels(true);

                        chart.getXAxis().setAxisMinimum(0);
                        chart.getXAxis().setAxisMaximum(allMonths.size());
                        chart.groupBars(0, 0.2f, 0.05f);
                        chart.invalidate();
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ExpenseReportActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ExpenseReportActivity.this, "Failed to load accounts", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
