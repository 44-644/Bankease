package com.example.bankease;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class BankAccountAdapter extends RecyclerView.Adapter<BankAccountAdapter.ViewHolder> {

    private final List<BankAccount> accountList;
    private final Context context;

    public BankAccountAdapter(Context context, List<BankAccount> accountList) {
        this.context = context;
        this.accountList = accountList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView bankName, accountNumber, currentBalance;
        Button deleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            bankName = itemView.findViewById(R.id.bankName);
            accountNumber = itemView.findViewById(R.id.accountNumber);
            currentBalance = itemView.findViewById(R.id.currentBalance);
            deleteBtn = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(BankAccount acc, Context context, List<BankAccount> accountList, BankAccountAdapter adapter) {
            if (acc != null) {
                bankName.setText(acc.getBankName());
                accountNumber.setText("Account #: " + acc.getAccountNumber());
                currentBalance.setText("Balance: $" + acc.getCurrentBalance());

                deleteBtn.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) return;

                    // ✅ Use bankName and accountId for deletion
                    String bankKey = acc.getBankName().replace(" ", "_");
                    String accountId = acc.getAccountId();
                    String path = "BankAccounts/" + bankKey + "/" + accountId;

                    Log.d("BankAccountAdapter", "Deleting: " + path);

                    FirebaseDatabase.getInstance().getReference(path)
                            .removeValue()
                            .addOnSuccessListener(unused -> {
                                accountList.remove(position);
                                adapter.notifyItemRemoved(position);
                                Log.i("BankAccountAdapter", "Deleted from Firebase: " + path);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("BankAccountAdapter", "Failed to delete: " + path, e);
                            });
                });
            }
        }
    }

    @NonNull
    @Override
    public BankAccountAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bank_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BankAccountAdapter.ViewHolder holder, int position) {
        holder.bind(accountList.get(position), context, accountList, this);
    }

    @Override
    public int getItemCount() {
        return accountList != null ? accountList.size() : 0;
    }
}
