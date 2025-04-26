package com.example.bankease;

public class PasswordValidator {
    public static boolean isStrongPassword(String password) {
        // At least 8 characters
        if (password.length() < 8) return false;

        // At least one digit
        if (!password.matches(".*\\d.*")) return false;

        // At least one special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) return false;

        // At least one uppercase letter
        if (!password.matches(".*[A-Z].*")) return false;

        return true;
    }

    public static String getPasswordRequirements() {
        return "Password must contain:\n" +
                "- At least 8 characters\n" +
                "- At least one number\n" +
                "- At least one special character\n" +
                "- At least one uppercase letter";
    }
}