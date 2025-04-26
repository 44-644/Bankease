package com.example.bankease;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends BaseAdapter {

    private final Context context;
    private final List<ExpenseReportActivity.Transaction> transactionList;

    public TransactionAdapter(Context context, List<ExpenseReportActivity.Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
    }

    @Override
    public int getCount() {
        return transactionList.size();
    }

    @Override
    public Object getItem(int i) {
        return transactionList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int pos, View view, ViewGroup parent) {
        view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);

        TextView line1 = view.findViewById(android.R.id.text1);
        TextView line2 = view.findViewById(android.R.id.text2);

        ExpenseReportActivity.Transaction txn = transactionList.get(pos);
        line1.setText("From: " + txn.fromAccount + " → " + txn.toAccount);
        line2.setText("₹" + txn.amount + " on " +
                new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(txn.timestamp)));

        return view;
    }
}
