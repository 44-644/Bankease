package com.example.bankease;

/**
 * Model class representing a linked bank account for a user.
 */
public class BankAccount {

    private String accountId;        // Firebase unique ID (optional for local usage)
    private String bankName;
    private String accountNumber;
    private String accountType;
    private String currentBalance;   // Stored as string for Firebase compatibility
    private String userId;           // UID of the user who owns the account

    // Required empty constructor for Firebase
    public BankAccount() {}

    public BankAccount(String accountId, String bankName, String accountNumber, String accountType,
                       String currentBalance, String userId) {
        this.accountId = accountId;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.currentBalance = currentBalance;
        this.userId = userId;
    }

    // Getters
    public String getAccountId() {
        return accountId;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getCurrentBalance() {
        return currentBalance;
    }

    public String getUserId() {
        return userId;
    }

    // Setters
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public void setCurrentBalance(String currentBalance) {
        this.currentBalance = currentBalance;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Debug print
    @Override
    public String toString() {
        return "BankAccount{" +
                "accountId='" + accountId + '\'' +
                ", bankName='" + bankName + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", accountType='" + accountType + '\'' +
                ", currentBalance='" + currentBalance + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
